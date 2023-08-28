package co.yuheng.simplemail;

import java.util.HashMap;
import java.util.List;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class Controller {

	static Gson gson = new Gson();
	static JedisPool jedisPool = new JedisPool("redis", 6379);
	static HashMap<String, MailboxCredential> credentials = new HashMap<>();

	static String redisCredentialPrefix = "simplemail_credential:";
	static String redisMailPrefix = "simplemail_mail:";

	public static ResponseEntity<String> authenticateHasError(String token) {
		if (credentials.containsKey(token)) {
			return null;
		}
		try (Jedis jedis = jedisPool.getResource()) {
			if (!jedis.exists(redisCredentialPrefix + token)) {
				return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid token");
			}
			MailboxCredential credential = gson.fromJson(jedis.get(redisCredentialPrefix + token), MailboxCredential.class);
			credentials.put(token, credential);
		}
		return null;
	}

	@PostMapping("/login")
	public ResponseEntity<String> handleLogin(@RequestBody MailboxCredential credential) {
		Pop3Handler handler = Pop3Handler.getInstance();
		try (Jedis jedis = jedisPool.getResource()) {
			if (handler.validate(credential)) {
				String token = credential.hashString();
				credentials.put(token, credential);
				jedis.set(redisCredentialPrefix + token, gson.toJson(credential));
				return ResponseEntity.ok(gson.toJson(credential));
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body(e.getMessage());
		}
		return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid credential");
	}

	@GetMapping("/login")
	public ResponseEntity<String> handleLoginGet(@RequestHeader("Authorization") String token) {
		ResponseEntity<String> error = authenticateHasError(token);
		if (error != null) {
			return error;
		}
		MailboxCredential credential = credentials.get(token);
		return ResponseEntity.ok(gson.toJson(credential));
	}
	
	class MailList {
		int page;
		int pageSize;
		int total;
		List<Mail> mails;
	}

	@GetMapping("/mails_count")
	public ResponseEntity<String> handleCount(@RequestHeader("Authorization") String token) {
		ResponseEntity<String> error = authenticateHasError(token);
		if (error != null) {
			return error;
		}
		MailboxCredential credential = credentials.get(token);
		Pop3Handler handler = Pop3Handler.getInstance();
		try {
			int total = handler.getTotalNum(credential);
			return ResponseEntity.ok(gson.toJson(total));
		} catch (RuntimeException e) {
			e.printStackTrace();
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("/mails")
	public ResponseEntity<String> handleMails(
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "per_page", defaultValue = "10") int pageSize,
			@RequestHeader("Authorization") String token) {
		ResponseEntity<String> error = authenticateHasError(token);
		if (error != null) {
			return error;
		}
		MailboxCredential credential = credentials.get(token);
		Pop3Handler handler = Pop3Handler.getInstance();
		try {
			List<Mail> mails = handler.list(credential, page, pageSize);
			MailList mailList = new MailList();
			mailList.page = page;
			mailList.pageSize = pageSize;
			mailList.total = handler.getTotalNum(credential);
			mailList.mails = mails;
			return ResponseEntity.ok(gson.toJson(mailList));
		} catch (RuntimeException e) {
			e.printStackTrace();
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("/mails/{mail_id}")
	public ResponseEntity<String> handleMail(
			@PathVariable("mail_id") String mailIdStr,
			@RequestHeader("Authorization") String token) {
		ResponseEntity<String> error = authenticateHasError(token);
		if (error != null) {
			return error;
		}
		MailboxCredential credential = credentials.get(token);
		int mailId = -1;
		try {
			mailId = Integer.parseInt(mailIdStr);
		} catch (NumberFormatException e) {
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body("Invalid mail id");
		}

		try (Jedis jedis = jedisPool.getResource()){
			String mailKey = redisMailPrefix + token + "_" + mailId;
			if (jedis.exists(mailKey)) {
				return ResponseEntity.ok(jedis.get(mailKey));
			}
			Pop3Handler handler = Pop3Handler.getInstance();
			Mail m = handler.getMail(credential, mailId);
			if (m != null) {
				jedis.set(mailKey, m.toString());
				return ResponseEntity.ok(m.toString());
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body(e.getMessage());
		}
		return ResponseEntity.status(Response.SC_INTERNAL_SERVER_ERROR).body("Unknown error");
	}

}