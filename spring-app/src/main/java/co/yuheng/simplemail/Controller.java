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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class Controller {

	static Gson gson = new Gson();
	static HashMap<String, MailboxCredential> credentials = new HashMap<>();

	@PostMapping("/login")
	public ResponseEntity<String> handleLogin(@RequestBody MailboxCredential credential) {
		Pop3Handler handler = Pop3Handler.getInstance();
		try {
			if (handler.validate(credential)) {
				String token = credential.hashString();
				credentials.put(token, credential);
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
		if (!credentials.containsKey(token)) {
			return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid token");
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
		if (!credentials.containsKey(token)) {
			return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid token");
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
		if (!credentials.containsKey(token)) {
			return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid token");
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
		if (!credentials.containsKey(token)) {
			return ResponseEntity.status(Response.SC_UNAUTHORIZED).body("Invalid token");
		}
		MailboxCredential credential = credentials.get(token);
		int mailId = -1;
		try {
			mailId = Integer.parseInt(mailIdStr);
		} catch (NumberFormatException e) {
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body("Invalid mail id");
		}

		Pop3Handler handler = Pop3Handler.getInstance();
		try {
			Mail m = handler.getMail(credential, mailId);
			if (m != null) {
				return ResponseEntity.ok(m.toString());
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			return ResponseEntity.status(Response.SC_BAD_REQUEST).body(e.getMessage());
		}
		return ResponseEntity.status(Response.SC_INTERNAL_SERVER_ERROR).body("Unknown error");
	}

}