package com.karate.tournament.service.impl;

import com.karate.tournament.entity.AccountRequest;
import com.karate.tournament.entity.AppUser;
import com.karate.tournament.entity.Organization;
import com.karate.tournament.service.AccountNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountNotificationServiceImpl implements AccountNotificationService {
  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  @Value("${app.mail.from:no-reply@karate-ops.local}")
  private final String fromAddress;

  public void accountApproved(AccountRequest request, String username, String temporaryPassword) {
    send(
        request.email,
        "Tai khoan Karate Ops da duoc cap",
        """
        Xin chao %s,

        Yeu cau cap tai khoan cho CLB %s da duoc duyet.
        Ten dang nhap: %s
        Mat khau tam thoi: %s

        Vui long dang nhap va bao quan thong tin tai khoan.
        """.formatted(request.displayName, request.organization.name, username, temporaryPassword)
    );
  }

  public void accountRejected(AccountRequest request) {
    send(
        request.email,
        "Yeu cau cap tai khoan Karate Ops chua duoc duyet",
        """
        Xin chao %s,

        Yeu cau cap tai khoan cho CLB %s da bi tu choi.
        Ly do: %s
        """.formatted(request.displayName, request.organization.name, safeReason(request.decisionNote))
    );
  }

  public void directAccountCreated(Organization organization, AppUser user, String temporaryPassword) {
    send(
        user.email,
        "Tai khoan Karate Ops moi",
        """
        Xin chao %s,

        Quan tri vien CLB %s da tao tai khoan cho ban.
        Ten dang nhap: %s
        Mat khau tam thoi: %s
        """.formatted(user.displayName, organization.name, user.username, temporaryPassword)
    );
  }

  private void send(String to, String subject, String body) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.info("Mail fallback to={} subject={} body={}", to, subject, body);
      return;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(to);
      message.setSubject(subject);
      message.setText(body);
      mailSender.send(message);
    } catch (MailException exception) {
      log.warn("Could not send email, falling back to log. to={} subject={} body={}", to, subject, body, exception);
    }
  }

  private String safeReason(String reason) {
    return reason == null || reason.isBlank() ? "Khong co ly do cu the" : reason;
  }
}
