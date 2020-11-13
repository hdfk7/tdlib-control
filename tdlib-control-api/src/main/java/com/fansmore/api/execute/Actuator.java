//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2020
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package com.fansmore.api.execute;

import com.fansmore.api.common.Constant;
import org.apache.commons.lang3.StringUtils;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Example class for TDLib usage from Java.
 */
public final class Actuator {
    private Client client = null;

    private TdApi.AuthorizationState authorizationState = null;
    private volatile boolean haveAuthorization = false;
    private volatile boolean needQuit = false;
    private volatile boolean canQuit = false;

    private final Client.ResultHandler defaultHandler = new DefaultHandler();

    private final Lock authorizationLock = new ReentrantLock();
    private final Condition gotAuthorization = authorizationLock.newCondition();

    private final ConcurrentMap<Integer, TdApi.User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, TdApi.BasicGroup> basicGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, TdApi.Supergroup> supergroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, TdApi.SecretChat> secretChats = new ConcurrentHashMap<>();

    private final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
    private boolean haveFullMainChatList = false;

    private final ConcurrentMap<Integer, TdApi.UserFullInfo> usersFullInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, TdApi.BasicGroupFullInfo> basicGroupsFullInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, TdApi.SupergroupFullInfo> supergroupsFullInfo = new ConcurrentHashMap<>();

    private final String newLine = System.getProperty("line.separator");

    private volatile boolean waitCode = false;
    private volatile boolean waitUsername = false;
    private volatile boolean waitPassword = false;

    private TdApi.AddProxy proxy = null;
    private final BlockingDeque<String> commandQueue = new LinkedBlockingDeque<>();
    private final String phone;
    private String code;
    private String firstName;
    private String lastName;
    private String password;

    public Actuator(String phone) {
        this(phone, "127.0.0.1", 1080, new TdApi.ProxyTypeSocks5());
    }

    public Actuator(String phone, String url, int port, TdApi.ProxyType type) {
        this.phone = phone;
        if (StringUtils.isNotEmpty(url) && port > 0 && type != null) {
            proxy = new TdApi.AddProxy(url, port, true, type);
        }
    }

    public void code(String code) {
        if (!canQuit) {
            this.code = code;
            client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler());
        }
    }

    public void username(String username) {
        if (!canQuit) {
            final String[] name = username.split(" ");
            this.firstName = name[0];
            this.lastName = name[1];
            client.send(new TdApi.RegisterUser(firstName, lastName), new AuthorizationRequestHandler());
        }
    }

    public void password(String password) {
        if (!canQuit) {
            this.password = password;
            client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler());
        }
    }

    public void command(String command) {
        if (!canQuit) {
            if ("q".equals(command)) {
                close();
            }
            commandQueue.add(command);
        }
    }

    public void close() {
        if (!canQuit) {
            runCommand("q");
        }
    }

    public void cleanup() {
        this.code = null;
        this.firstName = null;
        this.lastName = null;
        this.password = null;
    }

    private void print(String str) {
        System.out.println(str);
    }

    private void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isRemoved = mainChatList.remove(new OrderedChat(chat.id, position));
                        assert isRemoved;
                    }
                }

                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isAdded = mainChatList.add(new OrderedChat(chat.id, position));
                        assert isAdded;
                    }
                }
            }
        }
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            this.authorizationState = authorizationState;
        }
        switch (this.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.TdlibParameters parameters = new TdApi.TdlibParameters();
                parameters.databaseDirectory = "data/" + phone;
                parameters.useMessageDatabase = true;
                parameters.useSecretChats = true;
                parameters.apiId = 2483379;
                parameters.apiHash = "22c0d60091142dcf1a8984285a71210e";
                parameters.systemLanguageCode = "en";
                parameters.deviceModel = "Desktop";
                parameters.applicationVersion = "1.0";
                parameters.enableStorageOptimizer = true;

                client.send(new TdApi.SetTdlibParameters(parameters), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR:
                client.send(new TdApi.CheckDatabaseEncryptionKey(), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR: {
                String link = ((TdApi.AuthorizationStateWaitOtherDeviceConfirmation) this.authorizationState).link;
                print("Please confirm this login link on another device: " + link);
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                Constant.CACHED_THREAD_POOL.execute(() -> {
                    if (waitCode) return;
                    waitCode = true;
                    long wait = System.currentTimeMillis();
                    while (StringUtils.isEmpty(code)) {
                        print("正在等待验证码 " + phone);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (System.currentTimeMillis() - wait > 1000L * 60 * 1) {
                            print("等待验证码超时 " + phone);
                            ActuatorManager.getInstance().close(phone);
                            break;
                        }
                    }
                    waitCode = false;
                });
                break;
            }
            case TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR: {
                Constant.CACHED_THREAD_POOL.execute(() -> {
                    if (waitUsername) return;
                    waitUsername = true;
                    long wait = System.currentTimeMillis();
                    while (StringUtils.isEmpty(firstName)) {
                        print("正在等待username " + phone);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (System.currentTimeMillis() - wait > 1000L * 60 * 1) {
                            print("正在等待username超时 " + phone);
                            ActuatorManager.getInstance().close(phone);
                            break;
                        }
                    }
                    waitUsername = false;
                });
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                Constant.CACHED_THREAD_POOL.execute(() -> {
                    if (waitPassword) return;
                    waitPassword = true;
                    long wait = System.currentTimeMillis();
                    while (StringUtils.isEmpty(password)) {
                        print("正在等待password " + phone);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (System.currentTimeMillis() - wait > 1000L * 60 * 1) {
                            print("正在等待password超时 " + phone);
                            ActuatorManager.getInstance().close(phone);
                            break;
                        }
                    }
                    waitPassword = false;
                });
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                } finally {
                    authorizationLock.unlock();
                }
                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                haveAuthorization = false;
                print("Logging out");
                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                haveAuthorization = false;
                print("Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
                print("Closed");
                if (!needQuit) {
                    client = Client.create(new UpdateHandler(), null, null, proxy); // recreate client after previous has closed
                } else {
                    canQuit = true;
                }
                break;
            default:
                print("Unsupported authorization state:" + newLine + this.authorizationState);
        }
    }

    private int toInt(String arg) {
        int result = 0;
        try {
            result = Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
        }
        return result;
    }

    private long toLong(String arg) {
        long chatId = 0;
        try {
            chatId = Long.parseLong(arg);
        } catch (NumberFormatException ignored) {
        }
        return chatId;
    }

    private double toDouble(String arg) {
        double chatId = 0;
        try {
            chatId = Double.parseDouble(arg);
        } catch (NumberFormatException ignored) {
        }
        return chatId;
    }

    private void runCommand(String command) {
        String[] commands = command.split(" ", 2);
        try {
            switch (commands[0]) {
                case "gcs": {
                    int limit = 20;
                    if (commands.length > 1) {
                        limit = toInt(commands[1]);
                    }
                    getMainChatList(limit);
                    break;
                }
                case "me": {
                    client.send(new TdApi.GetMe(), defaultHandler);
                    break;
                }
                case "sm": {
                    String[] args = commands[1].split(" ", 2);
                    sendMessage(toLong(args[0]), args[1]);
                    break;
                }
                case "lo": {
                    haveAuthorization = false;
                    client.send(new TdApi.LogOut(), defaultHandler);
                    break;
                }
                case "q": {
                    needQuit = true;
                    haveAuthorization = false;
                    // 代码有点丑 一次调用可能无法关闭
                    for (int i = 0; i < 5; i++) {
                        client.send(new TdApi.Close(), defaultHandler);
                    }
                    break;
                }
                case "搜索聊天": {
                    client.send(new TdApi.SearchPublicChat(commands[1]), defaultHandler);
                    break;
                }
                case "模糊搜索聊天": {
                    client.send(new TdApi.SearchPublicChats(commands[1]), defaultHandler);
                    break;
                }
                case "搜索附近人和群": {
                    String[] c = commands[1].split(" ");
                    // 经度 纬度 范围
                    //scn 30.590878 104.071428 0
                    TdApi.Location location = new TdApi.Location(toDouble(c[0]), toDouble(c[1]), Integer.parseInt(c[2]));
                    client.send(new TdApi.SearchChatsNearby(location), defaultHandler);
                    break;
                }
                case "创建个人私聊": {
                    client.send(new TdApi.CreateNewSecretChat(Integer.parseInt(commands[1])), defaultHandler);
                    break;
                }
                case "创建群组": {
                    // 主题 userId
                    // 创建群组 哈😁哈100 1241621759
                    String[] c = commands[1].split(" ");
                    String title = c[0];
                    int[] user = new int[c.length - 1];
                    for (int i = 1; i < c.length; i++) {
                        user[i - 1] = Integer.parseInt(c[i]);
                    }
                    client.send(new TdApi.CreateNewBasicGroupChat(user, title), defaultHandler);
                    break;
                }
                case "创建超级群组": {
                    // 主题 是否是频道 群描述 经度 纬度 范围 地址
                    // 创建超级群组 哈😁哈1 false 我不是机器人 30.590878 104.071428 0 菁蓉汇
                    String[] c = commands[1].split(" ");
                    String title = c[0];
                    boolean isChannel = Boolean.parseBoolean(c[1]);
                    String description = c[2];
                    TdApi.Location location = new TdApi.Location(toDouble(c[3]), toDouble(c[4]), toInt(c[5]));
                    TdApi.ChatLocation chatLocation = new TdApi.ChatLocation(location, c[6]);
                    client.send(new TdApi.CreateNewSupergroupChat(title, isChannel, description, chatLocation), defaultHandler);
                    break;
                }
                case "拉人进群": {
                    //拉人进群 -1001261372269 1241621759
                    String[] c = commands[1].split(" ");
                    long chatId = toLong(c[0]);
                    int[] member = new int[c.length - 1];
                    for (int i = 1; i < c.length; i++) {
                        member[i - 1] = toInt(c[i]);
                    }
                    client.send(new TdApi.AddChatMembers(chatId, member), defaultHandler);
                    break;
                }
                case "获取群成员资料": {
                    // -1001261372269
                    // 获取群成员资料 1316265672
                    String[] c = commands[1].split(" ");
                    int supergroupId = toInt(c[0]);
                    final TdApi.SupergroupMembersFilterSearch filterContacts = new TdApi.SupergroupMembersFilterSearch();
                    client.send(new TdApi.GetSupergroupMembers(supergroupId, filterContacts, 0, 200), defaultHandler);
                    break;
                }
                case "查询联系人资料": {
                    // 查询联系人资料 979386018
                    String[] c = commands[1].split(" ");
                    client.send(new TdApi.GetUserFullInfo(toInt(c[0])), defaultHandler);
                    break;
                }
                case "添加联系人到好友列表": {
                    // 1241621759 zhizhuxia1999 8618030501029
                    // 添加联系人 8618030501029 1241621759
                    String[] c = commands[1].split(" ");
                    TdApi.Contact contact = new TdApi.Contact();
                    contact.firstName = c[0];
                    contact.lastName = "";
                    contact.phoneNumber = c[0];
                    contact.userId = toInt(c[1]);
                    client.send(new TdApi.AddContact(contact, true), defaultHandler);
                    break;
                }
                case "批量导入联系人": {
                    // TODO 该方法调用后没有结果
                    // 批量导入联系人 +8618030501029
                    String[] c = commands[1].split(" ");
                    TdApi.Contact[] contacts = new TdApi.Contact[c.length];
                    for (int i = 0; i < c.length; i++) {
                        TdApi.Contact contact = new TdApi.Contact();
                        contact.phoneNumber = c[0];
                        contacts[i] = contact;
                    }
                    client.send(new TdApi.ImportContacts(contacts), defaultHandler);
                    break;
                }
                case "获取联系人列表": {
                    client.send(new TdApi.GetContacts(), defaultHandler);
                    break;
                }
                case "获取会话信息": {
                    // 获取会话信息 -1001316265672
                    client.send(new TdApi.GetChat(toLong(commands[1])), defaultHandler);
                    break;
                }
                case "删除会话列表": {
                    // 删除会话列表 -438749180 true true
                    String[] c = commands[1].split(" ");
                    long chatId = toLong(c[0]);
                    boolean removeFromChatList = Boolean.parseBoolean(c[1]);
                    boolean revoke = Boolean.parseBoolean(c[2]);
                    client.send(new TdApi.DeleteChatHistory(chatId, removeFromChatList, revoke), defaultHandler);
                    break;
                }
                case "删除超级会话列表": {
                    // 删除超级会话列表 1390906901
                    String[] c = commands[1].split(" ");
                    int supergroupId = toInt(c[0]);
                    client.send(new TdApi.DeleteSupergroup(supergroupId), defaultHandler);
                    break;
                }
                default:
                    System.err.println("Unsupported command: " + command);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            print("Not enough arguments");
        }
    }

    private void getMainChatList(final int limit) {
        synchronized (mainChatList) {
            if (!haveFullMainChatList && limit > mainChatList.size()) {
                // have enough chats in the chat list or chat list is too small
                long offsetOrder = Long.MAX_VALUE;
                long offsetChatId = 0;
                if (!mainChatList.isEmpty()) {
                    OrderedChat last = mainChatList.last();
                    offsetOrder = last.position.order;
                    offsetChatId = last.chatId;
                }
                client.send(new TdApi.GetChats(new TdApi.ChatListMain(), offsetOrder, offsetChatId, limit - mainChatList.size()), new Client.ResultHandler() {
                    @Override
                    public void onResult(TdApi.Object object) {
                        switch (object.getConstructor()) {
                            case TdApi.Error.CONSTRUCTOR:
                                print("");
                                print("Receive an error for GetChats:" + newLine + object);
                                break;
                            case TdApi.Chats.CONSTRUCTOR:
                                long[] chatIds = ((TdApi.Chats) object).chatIds;
                                if (chatIds.length == 0) {
                                    synchronized (mainChatList) {
                                        haveFullMainChatList = true;
                                    }
                                }
                                // chats had already been received through updates, let's retry request
                                getMainChatList(limit);
                                break;
                            default:
                                System.err.println("Receive wrong response from TDLib:" + newLine + object);
                        }
                    }
                });
                return;
            }

            // have enough chats in the chat list to answer request
            java.util.Iterator<OrderedChat> iter = mainChatList.iterator();
            print("");
            print("First " + limit + " chat(s) out of " + mainChatList.size() + " known chat(s):");
            for (int i = 0; i < limit; i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                synchronized (chat) {
                    print(chatId + ": " + chat.title);
                }
            }
            print("");
        }
    }

    private void sendMessage(long chatId, String message) {
        // initialize reply markup just for testing
        TdApi.InlineKeyboardButton[] row = {new TdApi.InlineKeyboardButton("https://telegram.org?1", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?2", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?3", new TdApi.InlineKeyboardButtonTypeUrl())};
        TdApi.ReplyMarkup replyMarkup = new TdApi.ReplyMarkupInlineKeyboard(new TdApi.InlineKeyboardButton[][]{row, row, row});

        TdApi.InputMessageContent content = new TdApi.InputMessageText(new TdApi.FormattedText(message, null), false, true);
        client.send(new TdApi.SendMessage(chatId, 0, 0, null, replyMarkup, content), defaultHandler);
    }

    public void run() throws Exception {
        // disable TDLib log
        Client.execute(new TdApi.SetLogVerbosityLevel(5));
        String path = "data/" + phone + "/";
        Files.createDirectories(Paths.get(path));
        if (Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile(path + "td.log", 1 << 27, false))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }
        // create client
        client = Client.create(new UpdateHandler(), null, null, proxy);

        // test Client.execute
        defaultHandler.onResult(Client.execute(new TdApi.GetTextEntities("@telegram /test_command https://telegram.org telegram.me @gif @test")));

        // main loop
        while (!needQuit) {
            // await authorization
            authorizationLock.lock();
            try {
                while (!haveAuthorization) {
                    gotAuthorization.await();
                }
            } finally {
                authorizationLock.unlock();
            }

            while (haveAuthorization) {
                try {
                    final String command = commandQueue.take();
                    runCommand(command);
                } catch (Exception e) {
                    print(e.getMessage());
                }
            }
        }
        while (!canQuit) {
            Thread.sleep(500);
        }
        ActuatorManager.getInstance().close(phone);
    }

    private class OrderedChat implements Comparable<OrderedChat> {
        final long chatId;
        final TdApi.ChatPosition position;

        OrderedChat(long chatId, TdApi.ChatPosition position) {
            this.chatId = chatId;
            this.position = position;
        }

        @Override
        public int compareTo(OrderedChat o) {
            if (this.position.order != o.position.order) {
                return o.position.order < this.position.order ? -1 : 1;
            }
            if (this.chatId != o.chatId) {
                return o.chatId < this.chatId ? -1 : 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            OrderedChat o = (OrderedChat) obj;
            return this.chatId == o.chatId && this.position.order == o.position.order;
        }
    }

    private class DefaultHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            print(object.toString());
        }
    }

    private class UpdateHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.UpdateAuthorizationState.CONSTRUCTOR:
                    onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState);
                    break;
                case TdApi.UpdateUser.CONSTRUCTOR:
                    TdApi.UpdateUser updateUser = (TdApi.UpdateUser) object;
                    users.put(updateUser.user.id, updateUser.user);
                    break;
                case TdApi.UpdateUserStatus.CONSTRUCTOR: {
                    TdApi.UpdateUserStatus updateUserStatus = (TdApi.UpdateUserStatus) object;
                    TdApi.User user = users.get(updateUserStatus.userId);
                    synchronized (user) {
                        user.status = updateUserStatus.status;
                    }
                    break;
                }
                case TdApi.UpdateBasicGroup.CONSTRUCTOR:
                    TdApi.UpdateBasicGroup updateBasicGroup = (TdApi.UpdateBasicGroup) object;
                    basicGroups.put(updateBasicGroup.basicGroup.id, updateBasicGroup.basicGroup);
                    break;
                case TdApi.UpdateSupergroup.CONSTRUCTOR:
                    TdApi.UpdateSupergroup updateSupergroup = (TdApi.UpdateSupergroup) object;
                    supergroups.put(updateSupergroup.supergroup.id, updateSupergroup.supergroup);
                    break;
                case TdApi.UpdateSecretChat.CONSTRUCTOR:
                    TdApi.UpdateSecretChat updateSecretChat = (TdApi.UpdateSecretChat) object;
                    secretChats.put(updateSecretChat.secretChat.id, updateSecretChat.secretChat);
                    break;

                case TdApi.UpdateNewChat.CONSTRUCTOR: {
                    TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) object;
                    TdApi.Chat chat = updateNewChat.chat;
                    synchronized (chat) {
                        chats.put(chat.id, chat);
                        TdApi.ChatPosition[] positions = chat.positions;
                        chat.positions = new TdApi.ChatPosition[0];
                        setChatPositions(chat, positions);
                    }
                    break;
                }
                case TdApi.UpdateChatTitle.CONSTRUCTOR: {
                    TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.title = updateChat.title;
                    }
                    break;
                }
                case TdApi.UpdateChatPhoto.CONSTRUCTOR: {
                    TdApi.UpdateChatPhoto updateChat = (TdApi.UpdateChatPhoto) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.photo = updateChat.photo;
                    }
                    break;
                }
                case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                    TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastMessage = updateChat.lastMessage;
                        setChatPositions(chat, updateChat.positions);
                    }
                    break;
                }
                case TdApi.UpdateChatPosition.CONSTRUCTOR: {
                    TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) object;
                    if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
                        break;
                    }

                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        int i;
                        for (i = 0; i < chat.positions.length; i++) {
                            if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                                break;
                            }
                        }
                        TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
                        int pos = 0;
                        if (updateChat.position.order != 0) {
                            new_positions[pos++] = updateChat.position;
                        }
                        for (int j = 0; j < chat.positions.length; j++) {
                            if (j != i) {
                                new_positions[pos++] = chat.positions[j];
                            }
                        }
                        assert pos == new_positions.length;

                        setChatPositions(chat, new_positions);
                    }
                    break;
                }
                case TdApi.UpdateChatReadInbox.CONSTRUCTOR: {
                    TdApi.UpdateChatReadInbox updateChat = (TdApi.UpdateChatReadInbox) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId;
                        chat.unreadCount = updateChat.unreadCount;
                    }
                    break;
                }
                case TdApi.UpdateChatReadOutbox.CONSTRUCTOR: {
                    TdApi.UpdateChatReadOutbox updateChat = (TdApi.UpdateChatReadOutbox) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId;
                    }
                    break;
                }
                case TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR: {
                    TdApi.UpdateChatUnreadMentionCount updateChat = (TdApi.UpdateChatUnreadMentionCount) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount;
                    }
                    break;
                }
                case TdApi.UpdateMessageMentionRead.CONSTRUCTOR: {
                    TdApi.UpdateMessageMentionRead updateChat = (TdApi.UpdateMessageMentionRead) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount;
                    }
                    break;
                }
                case TdApi.UpdateChatReplyMarkup.CONSTRUCTOR: {
                    TdApi.UpdateChatReplyMarkup updateChat = (TdApi.UpdateChatReplyMarkup) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.replyMarkupMessageId = updateChat.replyMarkupMessageId;
                    }
                    break;
                }
                case TdApi.UpdateChatDraftMessage.CONSTRUCTOR: {
                    TdApi.UpdateChatDraftMessage updateChat = (TdApi.UpdateChatDraftMessage) object;
                    TdApi.Chat chat = chats.get(updateChat.chatId);
                    synchronized (chat) {
                        chat.draftMessage = updateChat.draftMessage;
                        setChatPositions(chat, updateChat.positions);
                    }
                    break;
                }
                case TdApi.UpdateChatPermissions.CONSTRUCTOR: {
                    TdApi.UpdateChatPermissions update = (TdApi.UpdateChatPermissions) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.permissions = update.permissions;
                    }
                    break;
                }
                case TdApi.UpdateChatNotificationSettings.CONSTRUCTOR: {
                    TdApi.UpdateChatNotificationSettings update = (TdApi.UpdateChatNotificationSettings) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.notificationSettings = update.notificationSettings;
                    }
                    break;
                }
                case TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR: {
                    TdApi.UpdateChatDefaultDisableNotification update = (TdApi.UpdateChatDefaultDisableNotification) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.defaultDisableNotification = update.defaultDisableNotification;
                    }
                    break;
                }
                case TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR: {
                    TdApi.UpdateChatIsMarkedAsUnread update = (TdApi.UpdateChatIsMarkedAsUnread) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.isMarkedAsUnread = update.isMarkedAsUnread;
                    }
                    break;
                }
                case TdApi.UpdateChatIsBlocked.CONSTRUCTOR: {
                    TdApi.UpdateChatIsBlocked update = (TdApi.UpdateChatIsBlocked) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.isBlocked = update.isBlocked;
                    }
                    break;
                }
                case TdApi.UpdateChatHasScheduledMessages.CONSTRUCTOR: {
                    TdApi.UpdateChatHasScheduledMessages update = (TdApi.UpdateChatHasScheduledMessages) object;
                    TdApi.Chat chat = chats.get(update.chatId);
                    synchronized (chat) {
                        chat.hasScheduledMessages = update.hasScheduledMessages;
                    }
                    break;
                }

                case TdApi.UpdateUserFullInfo.CONSTRUCTOR:
                    TdApi.UpdateUserFullInfo updateUserFullInfo = (TdApi.UpdateUserFullInfo) object;
                    usersFullInfo.put(updateUserFullInfo.userId, updateUserFullInfo.userFullInfo);
                    break;
                case TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR:
                    TdApi.UpdateBasicGroupFullInfo updateBasicGroupFullInfo = (TdApi.UpdateBasicGroupFullInfo) object;
                    basicGroupsFullInfo.put(updateBasicGroupFullInfo.basicGroupId, updateBasicGroupFullInfo.basicGroupFullInfo);
                    break;
                case TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR:
                    TdApi.UpdateSupergroupFullInfo updateSupergroupFullInfo = (TdApi.UpdateSupergroupFullInfo) object;
                    supergroupsFullInfo.put(updateSupergroupFullInfo.supergroupId, updateSupergroupFullInfo.supergroupFullInfo);
                    break;
                default:
                    print("Unsupported update:" + newLine + object);
            }
        }
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.Error.CONSTRUCTOR:
                    cleanup();
                    System.err.println("Receive an error:" + newLine + object);
                    onAuthorizationStateUpdated(null); // repeat last action
                    break;
                case TdApi.Ok.CONSTRUCTOR:
                    // result is already received through UpdateAuthorizationState, nothing to do
                    break;
                default:
                    System.err.println("Receive wrong response from TDLib:" + newLine + object);
            }
        }
    }
}
