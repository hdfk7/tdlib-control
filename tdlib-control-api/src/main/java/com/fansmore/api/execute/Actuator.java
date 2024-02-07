package com.fansmore.api.execute;

import com.fansmore.api.common.Constant;
import com.fansmore.api.utils.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class Actuator {
    private Client client = null;
    private TdApi.AuthorizationState authorizationState = null;
    private volatile boolean haveAuthorization = false;
    private volatile boolean needQuit = false;
    private volatile boolean canQuit = false;
    private final Client.ResultHandler defaultHandler = new DefaultHandler();
    private final Lock authorizationLock = new ReentrantLock();
    private final Condition gotAuthorization;
    private final ConcurrentMap<Long, TdApi.User> users;
    private final ConcurrentMap<Long, TdApi.BasicGroup> basicGroups;
    private final ConcurrentMap<Long, TdApi.Supergroup> supergroups;
    private final ConcurrentMap<Integer, TdApi.SecretChat> secretChats;
    private final ConcurrentMap<Long, TdApi.Chat> chats;
    private final NavigableSet<OrderedChat> mainChatList;
    private boolean haveFullMainChatList;
    private final ConcurrentMap<Long, TdApi.UserFullInfo> usersFullInfo;
    private final ConcurrentMap<Long, TdApi.BasicGroupFullInfo> basicGroupsFullInfo;
    private final ConcurrentMap<Long, TdApi.SupergroupFullInfo> supergroupsFullInfo;
    private final String newLine;
    private final String commandsLine = "Enter command (gcs - GetChats, gc <chatId> - GetChat, me - GetMe, sm <chatId> <message> - SendMessage, lo - LogOut, q - Quit): ";
    private volatile String currentPrompt;

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
        this(phone, "127.0.0.1", 0, new TdApi.ProxyTypeSocks5());
    }

    public Actuator(String phone, String url, int port, TdApi.ProxyType type) {
        this.phone = phone;
        if (StringUtils.isNotEmpty(url) && port >= 0 && type != null) {
            proxy = new TdApi.AddProxy(url, port, true, type);
        }
    }

    public void code(String code) {
        print("校验验证码 " + this.phone + " " + code);
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

    private void print(String var0) {
        if (currentPrompt != null) {
            System.out.println();
        }

        System.out.println(var0);
        if (currentPrompt != null) {
            System.out.print(currentPrompt);
        }

    }

    private void setChatPositions(TdApi.Chat var0, TdApi.ChatPosition[] var1) {
        synchronized (mainChatList) {
            synchronized (var0) {
                TdApi.ChatPosition[] var4 = var0.positions;
                int var5 = var4.length;

                int var6;
                TdApi.ChatPosition var7;
                boolean var8;
                for (var6 = 0; var6 < var5; ++var6) {
                    var7 = var4[var6];
                    if (var7.list.getConstructor() == -400991316) {
                        var8 = mainChatList.remove(new OrderedChat(var0.id, var7));

                        assert var8;
                    }
                }

                var0.positions = var1;
                var4 = var0.positions;
                var5 = var4.length;

                for (var6 = 0; var6 < var5; ++var6) {
                    var7 = var4[var6];
                    if (var7.list.getConstructor() == -400991316) {
                        var8 = mainChatList.add(new OrderedChat(var0.id, var7));

                        assert var8;
                    }
                }
            }

        }
    }

    private void onAuthorizationStateUpdated(TdApi.AuthorizationState var0) {
        if (var0 != null) {
            authorizationState = var0;
        }

        String var2;
        switch (authorizationState.getConstructor()) {
            case -1868627365:
                var2 = promptString("Please enter email authentication code: ");
                client.send(new TdApi.CheckAuthenticationEmailCode(new TdApi.EmailAddressAuthenticationCode(var2)), new AuthorizationRequestHandler());
                break;
            case -1834871737:
                print("登陆成功 " + this.phone);
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                    break;
                } finally {
                    authorizationLock.unlock();
                }
            case 52643073:
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
            case 112238030:
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
            case 154449270:
                haveAuthorization = false;
                break;
            case 306402531:
                print("登陆账号 " + this.phone);
                client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), new AuthorizationRequestHandler());
                break;
            case 445855311:
                haveAuthorization = false;
                break;
            case 550350511:
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
            case 860166378:
                var2 = ((TdApi.AuthorizationStateWaitOtherDeviceConfirmation) authorizationState).link;
                System.out.println("Please confirm this login link on another device: " + var2);
                break;
            case 904720988:
                TdApi.SetTdlibParameters var1 = new TdApi.SetTdlibParameters();
                var1.databaseDirectory = "data/" + phone;
                var1.useMessageDatabase = true;
                var1.useSecretChats = true;
                var1.apiId = 94575;
                var1.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2";
                var1.systemLanguageCode = "en";
                var1.deviceModel = "Desktop";
                var1.applicationVersion = "1.0";
                client.send(var1, new AuthorizationRequestHandler());
                break;
            case 1040478663:
                var2 = promptString("Please enter email address: ");
                client.send(new TdApi.SetAuthenticationEmailAddress(var2), new AuthorizationRequestHandler());
                break;
            case 1526047584:
                print("Closed");
                if (!needQuit) {
                    client = Client.create(new UpdateHandler(), null, null, proxy);
                } else {
                    canQuit = true;
                }
                break;
            default:
                String var10001 = newLine;
                System.err.println("Unsupported authorization state:" + var10001 + String.valueOf(authorizationState));
        }

    }

    private int toInt(String var0) {
        int var1 = 0;

        try {
            var1 = Integer.parseInt(var0);
        } catch (NumberFormatException var3) {
        }
        return var1;
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

    private long getChatId(String var0) {
        long var1 = 0L;

        try {
            var1 = Long.parseLong(var0);
        } catch (NumberFormatException ignored) {
        }

        return var1;
    }

    private String promptString(String var0) {
        System.out.print(var0);
        currentPrompt = var0;
        BufferedReader var1 = new BufferedReader(new InputStreamReader(System.in));
        String var2 = "";

        try {
            var2 = var1.readLine();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

        currentPrompt = null;
        return var2;
    }

    private void runCommand(String command) {
        String[] commands = command.split(" ", 2);
        try {
            switch (commands[0]) {
                case "gcs": {
                    int var6 = 20;
                    if (commands.length > 1) {
                        var6 = toInt(commands[1]);
                    }

                    getMainChatList(var6);
                    break;
                }
                case "gc": {
                    client.send(new TdApi.GetChat(getChatId(commands[1])), defaultHandler);
                    break;
                }
                case "me": {
                    client.send(new TdApi.GetMe(), defaultHandler);
                    break;
                }
                case "sm": {
                    String[] var4 = commands[1].split(" ", 2);
                    sendMessage(getChatId(var4[0]), var4[1]);
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
                    client.send(new TdApi.Close(), defaultHandler);
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
//                    client.send(new TdApi.CreateNewBasicGroupChat(user, title), defaultHandler);
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
//                    client.send(new TdApi.CreateNewSupergroupChat(title, isChannel, description, chatLocation), defaultHandler);
                    break;
                }
                case "拉人进群": {
                    //拉人进群 -1001261372269 1241621759
                    String[] c = commands[1].split(" ");
                    long chatId = toLong(c[0]);
                    long[] member = new long[c.length - 1];
                    for (int i = 1; i < c.length; i++) {
                        member[i - 1] = toLong(c[i]);
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
//                    client.send(new TdApi.DeleteSupergroup(supergroupId), defaultHandler);
                    break;
                }
                default:
                    System.err.println("Unsupported command: " + command);
            }
        } catch (ArrayIndexOutOfBoundsException var5) {
            print("Not enough arguments");
        }
    }

    private void getMainChatList(final int var0) {
        synchronized (mainChatList) {
            if (!haveFullMainChatList && var0 > mainChatList.size()) {
                client.send(new TdApi.LoadChats(new TdApi.ChatListMain(), var0 - mainChatList.size()), new Client.ResultHandler() {
                    public void onResult(TdApi.Object var1) {
                        String var10001;
                        switch (var1.getConstructor()) {
                            case -1679978726:
                                if (((TdApi.Error) var1).code == 404) {
                                    synchronized (mainChatList) {
                                        haveFullMainChatList = true;
                                    }
                                } else {
                                    var10001 = newLine;
                                    System.err.println("Receive an error for LoadChats:" + var10001 + String.valueOf(var1));
                                }
                                break;
                            case -722616727:
                                getMainChatList(var0);
                                break;
                            default:
                                var10001 = newLine;
                                System.err.println("Receive wrong response from TDLib:" + var10001 + String.valueOf(var1));
                        }

                    }
                });
            } else {
                Iterator var2 = mainChatList.iterator();
                System.out.println();
                System.out.println("First " + var0 + " chat(s) out of " + mainChatList.size() + " known chat(s):");

                for (int var3 = 0; var3 < var0 && var3 < mainChatList.size(); ++var3) {
                    long var4 = ((OrderedChat) var2.next()).chatId;
                    TdApi.Chat var6 = (TdApi.Chat) chats.get(var4);
                    synchronized (var6) {
                        System.out.println("" + var4 + ": " + var6.title);
                    }
                }

                print("");
            }
        }
    }

    private void sendMessage(long var0, String var2) {
        TdApi.InlineKeyboardButton[] var3 = new TdApi.InlineKeyboardButton[]{new TdApi.InlineKeyboardButton("https://telegram.org?1", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?2", new TdApi.InlineKeyboardButtonTypeUrl()), new TdApi.InlineKeyboardButton("https://telegram.org?3", new TdApi.InlineKeyboardButtonTypeUrl())};
        TdApi.ReplyMarkupInlineKeyboard var4 = new TdApi.ReplyMarkupInlineKeyboard(new TdApi.InlineKeyboardButton[][]{var3, var3, var3});
        TdApi.InputMessageText var5 = new TdApi.InputMessageText(new TdApi.FormattedText(var2, (TdApi.TextEntity[]) null), (TdApi.LinkPreviewOptions) null, true);
        client.send(new TdApi.SendMessage(var0, 0L, (TdApi.InputMessageReplyTo) null, (TdApi.MessageSendOptions) null, var4, var5), defaultHandler);
    }

    public void run() throws Exception {
        Client.setLogMessageHandler(0, new LogMessageHandler());

        String path = "data/" + phone + "/";
        Files.createDirectories(Paths.get(path));
        try {
            Client.execute(new TdApi.SetLogVerbosityLevel(0));
            Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile(path + "tdlib.log", 134217728L, false)));
        } catch (Client.ExecutionException var5) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }
        client = Client.create(new UpdateHandler(), null, null, proxy);

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


    private void onFatalError(String var0) {
        AtomicLong var1 = new AtomicLong(Long.MAX_VALUE);

        final class ThrowError implements Runnable {
            private final String errorMessage;
            private final AtomicLong errorThrowTime;

            private ThrowError(String var1, AtomicLong var2) {
                this.errorMessage = var1;
                this.errorThrowTime = var2;
            }

            public void run() {
                if (!this.isDatabaseBrokenError(this.errorMessage) && !this.isDiskFullError(this.errorMessage) && !this.isDiskError(this.errorMessage)) {
                    this.errorThrowTime.set(System.currentTimeMillis());
                    throw new ThrowError.ClientError("TDLib fatal error: " + this.errorMessage);
                } else {
                    this.processExternalError();
                }
            }

            private void processExternalError() {
                this.errorThrowTime.set(System.currentTimeMillis());
                throw new ThrowError.ExternalClientError("Fatal error: " + this.errorMessage);
            }

            private boolean isDatabaseBrokenError(String var1) {
                return var1.contains("Wrong key or database is corrupted") || var1.contains("SQL logic error or missing database") || var1.contains("database disk image is malformed") || var1.contains("file is encrypted or is not a database") || var1.contains("unsupported file format") || var1.contains("Database was corrupted and deleted during execution and can't be recreated");
            }

            private boolean isDiskFullError(String var1) {
                return var1.contains("PosixError : No space left on device") || var1.contains("database or disk is full");
            }

            private boolean isDiskError(String var1) {
                return var1.contains("I/O error") || var1.contains("Structure needs cleaning");
            }

            final class ClientError extends Error {
                private ClientError(String var2) {
                    super(var2);
                }
            }

            final class ExternalClientError extends Error {
                public ExternalClientError(String var2) {
                    super(var2);
                }
            }
        }

        (new Thread(new ThrowError(var0, var1), "TDLib fatal error thread")).start();

        while (var1.get() >= System.currentTimeMillis() - 10000L) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var3) {
                Thread.currentThread().interrupt();
            }
        }

    }

    {
        gotAuthorization = authorizationLock.newCondition();
        users = new ConcurrentHashMap();
        basicGroups = new ConcurrentHashMap();
        supergroups = new ConcurrentHashMap();
        secretChats = new ConcurrentHashMap();
        chats = new ConcurrentHashMap();
        mainChatList = new TreeSet();
        haveFullMainChatList = false;
        usersFullInfo = new ConcurrentHashMap();
        basicGroupsFullInfo = new ConcurrentHashMap();
        supergroupsFullInfo = new ConcurrentHashMap();
        newLine = System.getProperty("line.separator");
        currentPrompt = null;
    }

    private class OrderedChat implements Comparable<OrderedChat> {
        final long chatId;
        final TdApi.ChatPosition position;

        OrderedChat(long var1, TdApi.ChatPosition var3) {
            this.chatId = var1;
            this.position = var3;
        }

        public int compareTo(OrderedChat var1) {
            if (this.position.order != var1.position.order) {
                return var1.position.order < this.position.order ? -1 : 1;
            } else if (this.chatId != var1.chatId) {
                return var1.chatId < this.chatId ? -1 : 1;
            } else {
                return 0;
            }
        }

        public boolean equals(Object var1) {
            OrderedChat var2 = (OrderedChat) var1;
            return this.chatId == var2.chatId && this.position.order == var2.position.order;
        }
    }

    private class AuthorizationRequestHandler implements Client.ResultHandler {
        private AuthorizationRequestHandler() {
        }

        public void onResult(TdApi.Object var1) {
            String var10001;
            switch (var1.getConstructor()) {
                case -1679978726:
                    var10001 = newLine;
                    System.err.println("Receive an error:" + var10001 + String.valueOf(var1));
                    onAuthorizationStateUpdated((TdApi.AuthorizationState) null);
                case -722616727:
                    break;
                default:
                    var10001 = newLine;
                    System.err.println("Receive wrong response from TDLib:" + var10001 + String.valueOf(var1));
            }

        }
    }

    private class UpdateHandler implements Client.ResultHandler {
        private UpdateHandler() {
        }

        public void onResult(TdApi.Object var1) {
            TdApi.Chat var7;
            switch (var1.getConstructor()) {
                case -2131461348:
                    TdApi.UpdateChatUnreadMentionCount var101 = (TdApi.UpdateChatUnreadMentionCount) var1;
                    var7 = (TdApi.Chat) chats.get(var101.chatId);
                    synchronized (var7) {
                        var7.unreadMentionCount = var101.unreadMentionCount;
                        break;
                    }
                case -2124399395:
                    TdApi.UpdateChatUnreadReactionCount var100 = (TdApi.UpdateChatUnreadReactionCount) var1;
                    var7 = (TdApi.Chat) chats.get(var100.chatId);
                    synchronized (var7) {
                        var7.unreadReactionCount = var100.unreadReactionCount;
                        break;
                    }
                case -2027228018:
                    TdApi.UpdateChatBlockList var99 = (TdApi.UpdateChatBlockList) var1;
                    var7 = (TdApi.Chat) chats.get(var99.chatId);
                    synchronized (var7) {
                        var7.blockList = var99.blockList;
                        break;
                    }
                case -1967909895:
                    TdApi.UpdateChatAvailableReactions var98 = (TdApi.UpdateChatAvailableReactions) var1;
                    var7 = (TdApi.Chat) chats.get(var98.chatId);
                    synchronized (var7) {
                        var7.availableReactions = var98.availableReactions;
                        break;
                    }
                case -1666903253:
                    TdApi.UpdateSecretChat var5 = (TdApi.UpdateSecretChat) var1;
                    secretChats.put(var5.secretChat.id, var5.secretChat);
                    break;
                case -1622010003:
                    TdApi.UpdateChatPermissions var97 = (TdApi.UpdateChatPermissions) var1;
                    var7 = (TdApi.Chat) chats.get(var97.chatId);
                    synchronized (var7) {
                        var7.permissions = var97.permissions;
                        break;
                    }
                case -1003239581:
                    TdApi.UpdateBasicGroup var69 = (TdApi.UpdateBasicGroup) var1;
                    basicGroups.put(var69.basicGroup.id, var69.basicGroup);
                    break;
                case -923244537:
                    TdApi.UpdateChatLastMessage var96 = (TdApi.UpdateChatLastMessage) var1;
                    var7 = (TdApi.Chat) chats.get(var96.chatId);
                    synchronized (var7) {
                        var7.lastMessage = var96.lastMessage;
                        setChatPositions(var7, var96.positions);
                        break;
                    }
                case -803163050:
                    TdApi.UpdateChatNotificationSettings var95 = (TdApi.UpdateChatNotificationSettings) var1;
                    var7 = (TdApi.Chat) chats.get(var95.chatId);
                    synchronized (var7) {
                        var7.notificationSettings = var95.notificationSettings;
                        break;
                    }
                case -797952281:
                    TdApi.UpdateChatReadInbox var94 = (TdApi.UpdateChatReadInbox) var1;
                    var7 = (TdApi.Chat) chats.get(var94.chatId);
                    synchronized (var7) {
                        var7.lastReadInboxMessageId = var94.lastReadInboxMessageId;
                        var7.unreadCount = var94.unreadCount;
                        break;
                    }
                case -643671870:
                    TdApi.UpdateChatActionBar var93 = (TdApi.UpdateChatActionBar) var1;
                    var7 = (TdApi.Chat) chats.get(var93.chatId);
                    synchronized (var7) {
                        var7.actionBar = var93.actionBar;
                        break;
                    }
                case -324713921:
                    TdApi.UpdateChatPhoto var92 = (TdApi.UpdateChatPhoto) var1;
                    var7 = (TdApi.Chat) chats.get(var92.chatId);
                    synchronized (var7) {
                        var7.photo = var92.photo;
                        break;
                    }
                case -252228282:
                    TdApi.UpdateMessageMentionRead var91 = (TdApi.UpdateMessageMentionRead) var1;
                    var7 = (TdApi.Chat) chats.get(var91.chatId);
                    synchronized (var7) {
                        var7.unreadMentionCount = var91.unreadMentionCount;
                        break;
                    }
                case -175405660:
                    TdApi.UpdateChatTitle var90 = (TdApi.UpdateChatTitle) var1;
                    var7 = (TdApi.Chat) chats.get(var90.chatId);
                    synchronized (var7) {
                        var7.title = var90.title;
                        break;
                    }
                case -76782300:
                    TdApi.UpdateSupergroup var70 = (TdApi.UpdateSupergroup) var1;
                    supergroups.put(var70.supergroup.id, var70.supergroup);
                    break;
                case -51197161:
                    TdApi.UpdateUserFullInfo var89 = (TdApi.UpdateUserFullInfo) var1;
                    usersFullInfo.put(var89.userId, var89.userFullInfo);
                    break;
                case -8979849:
                    TdApi.UpdateChatPosition var88 = (TdApi.UpdateChatPosition) var1;
                    if (var88.position.list.getConstructor() == -400991316) {
                        var7 = (TdApi.Chat) chats.get(var88.chatId);
                        synchronized (var7) {
                            int var81;
                            for (var81 = 0; var81 < var7.positions.length && var7.positions[var81].list.getConstructor() != -400991316; ++var81) {
                            }

                            TdApi.ChatPosition[] var10 = new TdApi.ChatPosition[var7.positions.length + (var88.position.order == 0L ? 0 : 1) - (var81 < var7.positions.length ? 1 : 0)];
                            int var11 = 0;
                            if (var88.position.order != 0L) {
                                var10[var11++] = var88.position;
                            }

                            for (int var12 = 0; var12 < var7.positions.length; ++var12) {
                                if (var12 != var81) {
                                    var10[var11++] = var7.positions[var12];
                                }
                            }

                            assert var11 == var10.length;

                            setChatPositions(var7, var10);
                        }
                    }
                    break;
                case -6473549:
                    TdApi.UpdateChatBackground var87 = (TdApi.UpdateChatBackground) var1;
                    var7 = (TdApi.Chat) chats.get(var87.chatId);
                    synchronized (var7) {
                        var7.background = var87.background;
                        break;
                    }
                case 348578785:
                    TdApi.UpdateChatPendingJoinRequests var86 = (TdApi.UpdateChatPendingJoinRequests) var1;
                    var7 = (TdApi.Chat) chats.get(var86.chatId);
                    synchronized (var7) {
                        var7.pendingJoinRequests = var86.pendingJoinRequests;
                        break;
                    }
                case 435539214:
                    TdApi.UpdateSupergroupFullInfo var8 = (TdApi.UpdateSupergroupFullInfo) var1;
                    supergroupsFullInfo.put(var8.supergroupId, var8.supergroupFullInfo);
                    break;
                case 464087707:
                    TdApi.UpdateChatDefaultDisableNotification var85 = (TdApi.UpdateChatDefaultDisableNotification) var1;
                    var7 = (TdApi.Chat) chats.get(var85.chatId);
                    synchronized (var7) {
                        var7.defaultDisableNotification = var85.defaultDisableNotification;
                        break;
                    }
                case 637226150:
                    TdApi.UpdateChatVideoChat var84 = (TdApi.UpdateChatVideoChat) var1;
                    var7 = (TdApi.Chat) chats.get(var84.chatId);
                    synchronized (var7) {
                        var7.videoChat = var84.videoChat;
                        break;
                    }
                case 708334213:
                    TdApi.UpdateChatReadOutbox var82 = (TdApi.UpdateChatReadOutbox) var1;
                    var7 = (TdApi.Chat) chats.get(var82.chatId);
                    synchronized (var7) {
                        var7.lastReadOutboxMessageId = var82.lastReadOutboxMessageId;
                        break;
                    }
                case 838063205:
                    TdApi.UpdateChatTheme var80 = (TdApi.UpdateChatTheme) var1;
                    var7 = (TdApi.Chat) chats.get(var80.chatId);
                    synchronized (var7) {
                        var7.themeName = var80.themeName;
                        break;
                    }
                case 942840008:
                    TdApi.UpdateMessageUnreadReactions var79 = (TdApi.UpdateMessageUnreadReactions) var1;
                    var7 = (TdApi.Chat) chats.get(var79.chatId);
                    synchronized (var7) {
                        var7.unreadReactionCount = var79.unreadReactionCount;
                        break;
                    }
                case 958468625:
                    TdApi.UpdateUserStatus var3 = (TdApi.UpdateUserStatus) var1;
                    TdApi.User var4 = (TdApi.User) users.get(var3.userId);
                    synchronized (var4) {
                        var4.status = var3.status;
                        break;
                    }
                case 1183394041:
                    TdApi.UpdateUser var2 = (TdApi.UpdateUser) var1;
                    users.put(var2.user.id, var2.user);
                    break;
                case 1309386144:
                    TdApi.UpdateChatReplyMarkup var78 = (TdApi.UpdateChatReplyMarkup) var1;
                    var7 = (TdApi.Chat) chats.get(var78.chatId);
                    synchronized (var7) {
                        var7.replyMarkupMessageId = var78.replyMarkupMessageId;
                        break;
                    }
                case 1391881151:
                    TdApi.UpdateBasicGroupFullInfo var83 = (TdApi.UpdateBasicGroupFullInfo) var1;
                    basicGroupsFullInfo.put(var83.basicGroupId, var83.basicGroupFullInfo);
                    break;
                case 1455190380:
                    TdApi.UpdateChatDraftMessage var77 = (TdApi.UpdateChatDraftMessage) var1;
                    var7 = (TdApi.Chat) chats.get(var77.chatId);
                    synchronized (var7) {
                        var7.draftMessage = var77.draftMessage;
                        setChatPositions(var7, var77.positions);
                        break;
                    }
                case 1468347188:
                    TdApi.UpdateChatIsMarkedAsUnread var76 = (TdApi.UpdateChatIsMarkedAsUnread) var1;
                    var7 = (TdApi.Chat) chats.get(var76.chatId);
                    synchronized (var7) {
                        var7.isMarkedAsUnread = var76.isMarkedAsUnread;
                        break;
                    }
                case 1622347490:
                    onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) var1).authorizationState);
                    break;
                case 1800406811:
                    TdApi.UpdateChatHasProtectedContent var75 = (TdApi.UpdateChatHasProtectedContent) var1;
                    var7 = (TdApi.Chat) chats.get(var75.chatId);
                    synchronized (var7) {
                        var7.hasProtectedContent = var75.hasProtectedContent;
                        break;
                    }
                case 1900174821:
                    TdApi.UpdateChatMessageAutoDeleteTime var74 = (TdApi.UpdateChatMessageAutoDeleteTime) var1;
                    var7 = (TdApi.Chat) chats.get(var74.chatId);
                    synchronized (var7) {
                        var7.messageAutoDeleteTime = var74.messageAutoDeleteTime;
                        break;
                    }
                case 2003849793:
                    TdApi.UpdateChatMessageSender var73 = (TdApi.UpdateChatMessageSender) var1;
                    var7 = (TdApi.Chat) chats.get(var73.chatId);
                    synchronized (var7) {
                        var7.messageSenderId = var73.messageSenderId;
                        break;
                    }
                case 2063799831:
                    TdApi.UpdateChatIsTranslatable var72 = (TdApi.UpdateChatIsTranslatable) var1;
                    var7 = (TdApi.Chat) chats.get(var72.chatId);
                    synchronized (var7) {
                        var7.isTranslatable = var72.isTranslatable;
                        break;
                    }
                case 2064958167:
                    TdApi.UpdateChatHasScheduledMessages var71 = (TdApi.UpdateChatHasScheduledMessages) var1;
                    var7 = (TdApi.Chat) chats.get(var71.chatId);
                    synchronized (var7) {
                        var7.hasScheduledMessages = var71.hasScheduledMessages;
                        break;
                    }
                case 2075757773:
                    TdApi.UpdateNewChat var6 = (TdApi.UpdateNewChat) var1;
                    var7 = var6.chat;
                    synchronized (var7) {
                        chats.put(var7.id, var7);
                        TdApi.ChatPosition[] var9 = var7.positions;
                        var7.positions = new TdApi.ChatPosition[0];
                        setChatPositions(var7, var9);
                    }
            }

        }
    }

    private class LogMessageHandler implements Client.LogMessageHandler {
        private LogMessageHandler() {
        }

        public void onLogMessage(int var1, String var2) {
            if (var1 == 0) {
                onFatalError(var2);
            } else {
                System.err.println(var2);
            }
        }
    }

    private class DefaultHandler implements Client.ResultHandler {
        private DefaultHandler() {
        }

        public void onResult(TdApi.Object var1) {
            print(JSONUtils.toJSONString(var1));
        }
    }
}
