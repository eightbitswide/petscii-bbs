package eu.sblendorio.bbs.tenants;

import eu.sblendorio.bbs.core.PetsciiThread;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.sblendorio.bbs.core.Colors.*;
import static eu.sblendorio.bbs.core.Keys.*;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.math.NumberUtils.toInt;

public class UserLogon extends PetsciiThread {

    public static final String dbFile = System.getProperty("user.home") + "/mydatabase.db";
    public static Properties properties;
    SecureRandom random;

    static {
        properties = new Properties();
        properties.setProperty("characterEncoding", "UTF-8");
        properties.setProperty("encoding", "\"UTF-8\"");
    }

    private Connection conn = null;
    private User user;

    public static class User {
        public final Long id;
        public final String nick;
        public final String realname;
        public final String email;

        public User(Long id, String nick, String realname, String email) {
            this.id = id;
            this.nick = nick;
            this.realname = realname;
            this.email = email;
        }
    }

    public static class Message {
        public final Long rowId;
        public final String userFrom;
        public final String userTo;
        public final Date dateTime;
        public boolean isRead;
        public final String subject;
        public final String message;
        public final boolean receiverExists;

        public Message(Long rowId, String userFrom, String userTo, Date dateTime, boolean isRead, String subject, String message, boolean receiverExists) {
            this.rowId = rowId;
            this.userFrom = userFrom;
            this.userTo = userTo;
            this.dateTime = dateTime;
            this.isRead = isRead;
            this.subject = subject;
            this.message = message;
            this.receiverExists = receiverExists;
        }
    }

    private void openConnection() throws Exception {
        if (!new File(dbFile).exists()) createDatabase(properties);
        if (conn == null || conn.isClosed())
            conn = DriverManager.getConnection("jdbc:sqlite:"+dbFile, properties);
    }

    public UserLogon() throws Exception {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            random = null;
        }
        openConnection();
    }

    @Override
    public void doLoop() throws Exception {
        String username;
        String password;
        cls();
        write(CASE_LOCK, LOWERCASE);
        write(LOGO);
        write(GREY3);
        do {
            do {
                print("USERID or 'NEW': ");
                flush(); username = readLine();
                if (isBlank(username)) return;
                if (equalsIgnoreCase(username, "new")) {
                    if (createNewUser()) {
                        write(GREEN); println("User created successfully.");
                    } else {
                        write(RED); println("Operation aborted.");
                    }
                    write(GREY3);
                    newline();
                }
            } while (equalsIgnoreCase(username, "new"));
            print("PASSWORD: ");
            flush(); password = readPassword();
            user = getUser(username, password);
            if (user == null) {
                write(RED);
                newline();
                write(REVON); println("Wrong username or password"); write(REVOFF);
                newline();
                write(GREY3);
            }
        } while (user == null);
        listMessages(false);
    }

    public void listUsers() throws Exception {
        cls();
        write(LOGO);
        write(GREY3);
        List<User> users = getUsers();
        int i = 0;
        for (User user: users) {
            ++i;
            write(CYAN);
            print(user.nick);
            write(GREY3);
            String realname = user.realname;
            if (isNotBlank(realname) && (user.nick + realname).length() > 36)
                realname = realname.substring(0, 33-user.nick.length())+"...";
            println((isBlank(realname) ? EMPTY : " (" + realname + ")"));
            if (i % 19 == 0 && i < users.size()) {
                newline();
                write(WHITE); print("ANY KEY FOR NEXT PAGE, '.' TO GO BACK "); write(GREY3);
                flush(); resetInput(); int ch = readKey(); resetInput();
                if (ch == '.') return;
                cls();
                write(LOGO);
                write(GREY3);
            }
        }
        newline();
        write(WHITE); print("PRESS ANY KEY TO GO BACK "); write(GREY3);
        flush(); resetInput(); readKey(); resetInput();
    }

    public void sendMessageGui() throws Exception {
        String receipt;
        String subject;
        String message;
        boolean ok = false;

        do {
            print("send to (? for user list): ");
            flush(); receipt = readLine();
            if (isBlank(receipt)) return;
            ok = existsUser(receipt);
            if (!ok && !"?".equals(receipt)) println("WARN: not existing user");
            if ("?".equals(receipt)) {
                listUsers();
                newline();
                newline();
            }
        } while (!ok);

        print("subject: ");
        flush(); subject = readLine();
        if (isBlank(subject)) return;

        print("message: ");
        flush(); message = readLine();

        sendMessage(user.nick, receipt, subject, message);
        newline(); write(WHITE);
        print("MESSAGE SENT - PRESS ANY KEY ");
        write(GREY3);
        flush(); resetInput(); readKey(); resetInput();
    }

    public void sendMessage(String from, String to, String subject, String message) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO messages (user_from, user_to, datetime, is_read, subject, message) values (?,?,?,?,?,?)")) {
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, 0);
            ps.setString(5, subject);
            ps.setString(6, message);
            ps.executeUpdate();
        }
    }

    public void listMessages(boolean onlyUnread) throws Exception {
        List<Message> messages = getMessages(user.nick, onlyUnread);

        int pagesize = 12;
        int offset = 0;
        String cmd = EMPTY;
        do {
            int size = messages.size();
            if (onlyUnread && size == 0) {
                onlyUnread = false;
                messages = getMessages(user.nick, onlyUnread);
                size = messages.size();
            }
            long unread = countUnreadMessages(user.nick);
            cls();
            write(LOGO);
            write(GREY3);
            println("Got " + size  + (onlyUnread ? " unread" : EMPTY) + " message" + (size != 1 ? "s" : EMPTY) + (onlyUnread || unread == 0 ? EMPTY : " (" + unread + " unread)") + ".");
            newline();
            for (int i=offset; i<Math.min(offset+pagesize, size); ++i) {
                Message m = messages.get(i);
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                DateFormat tf = new SimpleDateFormat("hh:mm:ssa");
                String nowString= df.format(new Date(System.currentTimeMillis()));
                String date = df.format(m.dateTime);
                if (date.equals(nowString)) date = tf.format(m.dateTime);
                String subject = isNotBlank(m.subject) ? m.subject : defaultString(m.message).replaceAll("[\r\n]", " ");
                if (isNotBlank(subject) && (1+(""+i).length()+1+10+1+m.userFrom.length()+1+m.subject.length() )>39)
                    subject = subject.substring(0,39-(1+(""+i).length()+1+10+1+m.userFrom.length()+1));
                write(LIGHT_RED); print((m.isRead ? " " : "*"));
                write(WHITE); print((i+1) + " ");
                write(GREY3); print(date + " ");
                write(m.receiverExists ? CYAN : RED); print(m.userFrom);
                print(" ");
                write(WHITE); print(subject);
                newline();
            }
            write(GREY3);
            write(WHITE);println("_______________________________________");write(GREY3);

            write(REVON); print(" U "); write(REVOFF); print(" List users ");
            write(REVON); print(" M "); write(REVOFF); print(" New message ");
            write(REVON); print(" . "); write(REVOFF); println(" Exit");

            write(REVON); print(" A "); write(REVOFF); print(" All messag ");
            write(REVON); print(" R "); write(REVOFF); println(" Only unread messages");

            write(REVON); print(" # "); write(REVOFF); print(" Read message number ");
            write(REVON); print(" K "); write(REVOFF); println(" Unsubscribe");

            write(REVON); print(" + "); write(REVOFF); print(" Next page ");
            write(REVON); print(" - "); write(REVOFF); print(" Prev page ");
            write(REVON); print(" P "); write(REVOFF); println(" Privacy");

            write(WHITE);println(StringUtils.repeat(chr(163), 39));write(GREY3);
            print("> ");
            flush(); cmd = readLine();
            cmd = trim(lowerCase(cmd));
            int index = toInt(cmd);
            if ("+".equals(cmd) && (offset+pagesize<size)) {
                offset += pagesize;
            } else if ("-".equals(cmd) && offset > 0) {
                offset -= pagesize;
            } else if ("u".equals(cmd)) {
                listUsers();
            } else if ("a".equals(cmd)) {
                onlyUnread = false;
            } else if ("r".equals(cmd)) {
                onlyUnread = true;
            } else if ("m".equals(cmd)) {
                sendMessageGui();
            } else if ("p".equals(cmd)) {
                showPrivacyPolicy();
            } else if (isNumeric(cmd) && index>0 && index<=size) {
                displayMessage(messages.get(index - 1));
            }
            messages = getMessages(user.nick, onlyUnread);
        } while (!".".equals(cmd));

    }

    public void displayMessage(Message m) throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        cls();
        write(LOGO);
        write(GREY3);
        println("From: "+ m.userFrom);
        println("To:   "+ m.userTo);
        println("Date: "+ df.format(m.dateTime));
        println("Subj: "+ m.subject);
        println(StringUtils.repeat(chr(163),39));
        println(m.message);
        markAsRead(m);
        newline();
        flush();
        resetInput(); readKey(); resetInput();
    }

    void markAsRead(Message m) throws Exception {
        m.isRead = true;
        try (PreparedStatement ps = conn.prepareStatement("update messages set is_read=1 where rowid=?")) {
            ps.setLong(1, m.rowId);
            ps.executeUpdate();
        }
    }

    public List<User> getUsers() throws Exception {
        List<User> result = new LinkedList<>();
        try (Statement s = conn.createStatement();
                ResultSet r = s.executeQuery("select id, nick, realname, email from users order by nick")) {
            while (r.next())
                result.add(new User(
                    r.getLong("id"),
                    r.getString("nick"),
                    r.getString("realname"),
                    r.getString("email")
            ));
        }
        return result;
    }

    public List<Message> getMessages(String userTo, boolean onlyUnread) throws Exception {
        List<Message> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("" +
                "SELECT messages.rowid, user_from, user_to, datetime, is_read, subject, message, id FROM messages LEFT JOIN users ON user_from=nick WHERE user_to=? "+
                (onlyUnread ? " AND is_read = 0 " : EMPTY) + " ORDER BY datetime DESC") ) {
            ps.setString(1, userTo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(new Message(
                            rs.getLong("rowid"),
                            rs.getString("user_from"),
                            rs.getString("user_to"),
                            new Date(rs.getLong("datetime")),
                            rs.getLong("is_read") != 0,
                            rs.getString("subject"),
                            rs.getString("message"),
                            rs.getString("id") != null
                    ));
            }
        }
        return result;
    }

    public boolean createNewUser() throws Exception {
        String username;
        String password;
        String realname;
        String email;
        boolean exists;
        newline();
        write(WHITE);
        println("ADDING NEW USER");
        println(StringUtils.repeat(chr(163), 15));
        write(GREY3);
        do {
            print("Username: ");
            flush(); username = readLine();
            if (isBlank(username)) return false;
            exists = existsUser(username) || "?".equals(username);
            if (exists) println("WARN: Username not available");
        } while (exists);
        print("Real name: "); flush(); realname = readLine();
        print("Email: "); flush(); email = readLine();
        do {
            print("Password: ");
            flush(); password = readPassword();
        } while (isBlank(password));
        write(LIGHT_RED); print("Do you confirm creation? (Y/N)"); write(GREY3);
        flush(); resetInput(); int key = readKey(); resetInput();
        newline();
        return (key=='Y' || key=='y') ? addUser(username, realname, email, password) : false;
    }

    public boolean addUser(String nick, String realname, String email, String password) throws Exception {
        if (existsUser(nick)) return false;

        PreparedStatement ps = conn.prepareStatement("insert into users (nick, realname, email, salt, password) values (?,?,?,?,?)");
        String salt = generateId();
        String hash = sha256Hex(salt + password);
        ps.setString(1, nick);
        ps.setString(2, realname);
        ps.setString(3, email);
        ps.setString(4, salt);
        ps.setString(5, hash);
        ps.execute();
        ps.close();
        return true;
    }

    public long countTotalMessages(String nick) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("select count(*) from messages where user_to=?")) {
            ps.setString(1, nick);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
    public long countUnreadMessages(String nick) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("select count(*) from messages where user_to=? and is_read=0")) {
            ps.setString(1, nick);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public boolean existsUser(String nick) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("select id, realname, email, salt, password from users where nick=?")) {
            ps.setString(1, nick);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User getUser(String nick, String givenPassword) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("select id, realname, email, salt, password from users where nick=?");) {
            ps.setString(1, nick);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = rs.next();
                if (!found) return null;
                String salt = defaultString(rs.getString("salt"));
                String password = defaultString(rs.getString("password"));
                if (!sha256Hex(salt + givenPassword).equals(password)) return null;
                Long id = rs.getLong("id");
                String realname = rs.getString("realname");
                String email = rs.getString("email");
                return new User(id, nick, realname, email);
            }
        }
    }

    public void createDatabase(Properties properties) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:"+dbFile, properties);
        Statement s;

        s = conn.createStatement();
        s.executeUpdate("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, nick TEXT, realname TEXT, email TEXT, salt text, password TEXT)");
        s.close();

        s = conn.createStatement();
        s.executeUpdate("CREATE TABLE messages (user_from TEXT, user_to TEXT, datetime INTEGER, is_read INTEGER, subject TEXT, message TEXT)");
        s.close();

        conn.close();
    }

    public static byte[] LOGO = new byte[] {32, 32, 32, 32, 32, 28, -84, 32, 32, 32, 32, 32, 32, 32, 32, 32,
            32, 32, 32, 32, 32, -104, -69, 32, 32, 32, 32, 32, 32, 32, 32, 32,
            32, 32, 5, -81, -81, -81, -81, -81, -81, -81, 13, 18, 28, -95, -65, -110,
            -84, 18, -69, -110, -69, 18, -69, -110, -66, 18, -68, -110, -66, 18, -65, -110,
            -65, -104, -84, 18, -94, -110, -95, 18, -65, -110, -66, 18, -65, -69, -110, -84,
            18, -94, -110, -95, 18, -65, -68, -95, -69, -110, -65, 18, -95, -110, 32, -95,
            32, 32, 32, 18, 5, -48, -46, -55, -42, -63, -44, -59, -110, 13, 18, 28,
            -95, -110, 32, -68, 18, -68, -110, 32, -68, -69, -95, 32, -65, 18, -65, -110,
            -104, -68, -94, -95, -65, -69, -65, 18, -66, -110, -68, -94, -95, 18, -69, -110,
            -69, 18, -95, -95, -95, -110, -68, -94, -95, 30, -94, -94, 32, 18, 5, -45,
            -59, -61, -44, -55, -49, -50, -110, 13, 32, 32, 32, 32, 32, 32, 32, 32,
            32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32,
            32, 32, 32, -104, -94, -66, 32, 32, 32, 5, -93, -93, -93, -93, -93, -93,
            -93, 13
    };

    public String generateId() {
        if (random == null) return UUID.randomUUID().toString();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String theHex = Integer.toHexString(bytes[i] & 0xFF).toLowerCase();
            sb.append(theHex.length() == 1 ? "0" + theHex : theHex);
        }
        return sb.toString();
    }

    public void showPrivacyPolicy() throws Exception {
        List<String> text = readTextFile("gdpr/privacy-statement.txt");
        if (isEmpty(text)) return;
        int size = text.size();
        int pagesize = 18;
        int offset = 0;
        int cmd = 0;
        do {
            cls();
            write(LOGO);
            write(GREY3);
            newline();
            for (int i = offset; i < Math.min(offset + pagesize, size); ++i) {
                println(text.get(i));

            }
            println();
            print("[");
            write(WHITE); print("+-");
            write(GREY3); print("] Next/Prev page  [");
            write(WHITE); print(".");
            write(GREY3); print("] EXIT");
            flush();
            resetInput();
            cmd = readKey();
            if (cmd == '.') {
                return;
            } else if (cmd == '-' && offset > 0) {
                offset -= pagesize;
            } else if (offset + pagesize < size) {
                offset += pagesize;
            }
        } while (true);
    }
}

