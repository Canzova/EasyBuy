package easybuy.user_service.service.email;

/**
 * Shared utility for building responsive, beautifully styled HTML email templates.
 */
public final class EmailTemplateHelper {

    private EmailTemplateHelper() {
        // Utility class
    }

    /**
     * Builds a responsive HTML template containing the OTP code for password reset.
     *
     * @param otp The 6-digit one-time password
     * @return HTML string formatted for email clients
     */
    public static String buildOtpHtmlTemplate(String otp) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>EasyBuy - Password Reset OTP</title>
              <style>
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  background-color: #f3f4f6;
                  margin: 0;
                  padding: 24px;
                }
                .email-card {
                  max-width: 480px;
                  margin: 0 auto;
                  background-color: #ffffff;
                  border-radius: 12px;
                  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
                  padding: 36px 32px;
                  border: 1px solid #e5e7eb;
                }
                .brand-title {
                  text-align: center;
                  font-size: 24px;
                  font-weight: 800;
                  color: #111827;
                  letter-spacing: -0.5px;
                  margin-bottom: 24px;
                }
                .brand-title span {
                  color: #3b82f6;
                }
                .greeting {
                  font-size: 16px;
                  color: #374151;
                  margin-bottom: 12px;
                }
                .instructions {
                  font-size: 14px;
                  color: #4b5563;
                  line-height: 1.6;
                  margin-bottom: 24px;
                }
                .otp-box {
                  text-align: center;
                  background-color: #eff6ff;
                  border: 2px dashed #3b82f6;
                  border-radius: 8px;
                  padding: 18px;
                  font-size: 32px;
                  font-weight: 800;
                  letter-spacing: 8px;
                  color: #1d4ed8;
                  margin: 24px 0;
                }
                .warning {
                  font-size: 13px;
                  color: #dc2626;
                  background-color: #fef2f2;
                  border-left: 3px solid #dc2626;
                  padding: 10px 14px;
                  border-radius: 4px;
                  margin-bottom: 20px;
                }
                .footer {
                  text-align: center;
                  font-size: 12px;
                  color: #9ca3af;
                  border-top: 1px solid #f3f4f6;
                  padding-top: 20px;
                  margin-top: 24px;
                }
              </style>
            </head>
            <body>
              <div class="email-card">
                <div class="brand-title">Easy<span>Buy</span></div>
                <div class="greeting">Hello,</div>
                <div class="instructions">
                  You requested to reset the password for your EasyBuy account. Use the following One-Time Password (OTP) to proceed:
                </div>
                <div class="otp-box">""" + otp + """
                </div>
                <div class="warning">
                  This OTP is confidential and expires in 5 minutes. Do not share it with anyone.
                </div>
                <div class="instructions">
                  If you did not request this, please disregard this email. Your password will remain unchanged.
                </div>
                <div class="footer">
                  &copy; EasyBuy Microservices &bull; Automated Security Notification
                </div>
              </div>
            </body>
            </html>
            """;
    }
}
