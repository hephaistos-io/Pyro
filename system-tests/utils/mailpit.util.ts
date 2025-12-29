/**
 * Mailpit API utilities for E2E testing.
 * Mailpit provides a web UI at localhost:8025 and an API for querying emails.
 */

const MAILPIT_API = 'http://localhost:8025/api/v1';

/**
 * Represents a Mailpit email message.
 */
export interface MailpitMessage {
    ID: string;
    MessageID: string;
    From: {
        Name: string;
        Address: string;
    };
    To: Array<{
        Name: string;
        Address: string;
    }>;
    Subject: string;
    Date: string;
    Size: number;
    Attachments: number;
}

/**
 * Represents the full content of a Mailpit message.
 */
export interface MailpitMessageContent {
    ID: string;
    MessageID: string;
    Subject: string;
    From: {
        Name: string;
        Address: string;
    };
    To: Array<{
        Name: string;
        Address: string;
    }>;
    HTML: string;
    Text: string;
}

/**
 * Search response from Mailpit.
 */
interface MailpitSearchResponse {
    messages: MailpitMessage[];
    count: number;
    start: number;
    total: number;
}

/**
 * Gets the latest email sent to a specific recipient.
 * @param email - The recipient email address
 * @returns The latest email or null if none found
 */
export async function getLatestEmailForRecipient(email: string): Promise<MailpitMessage | null> {
    const response = await fetch(`${MAILPIT_API}/search?query=to:${encodeURIComponent(email)}`);
    if (!response.ok) {
        throw new Error(`Mailpit API error: ${response.status} ${response.statusText}`);
    }
    const data: MailpitSearchResponse = await response.json();
    return data.messages?.[0] || null;
}

/**
 * Gets the full content of an email by its ID.
 * @param id - The Mailpit message ID
 * @returns The full email content
 */
export async function getEmailContent(id: string): Promise<MailpitMessageContent> {
    const response = await fetch(`${MAILPIT_API}/message/${id}`);
    if (!response.ok) {
        throw new Error(`Mailpit API error: ${response.status} ${response.statusText}`);
    }
    return response.json();
}

/**
 * Extracts a link matching a pattern from an email.
 * @param email - The recipient email address
 * @param linkPattern - Regex pattern to match the link
 * @returns The matching URL or null if not found
 */
export async function extractLinkFromEmail(
    email: string,
    linkPattern: RegExp
): Promise<string | null> {
    const message = await getLatestEmailForRecipient(email);
    if (!message) return null;

    const content = await getEmailContent(message.ID);
    const htmlContent = content.HTML || content.Text;

    const match = htmlContent.match(linkPattern);
    return match ? match[0] : null;
}

/**
 * Clears all emails from Mailpit.
 * Useful for test cleanup.
 */
export async function clearMailbox(): Promise<void> {
    await fetch(`${MAILPIT_API}/messages`, {method: 'DELETE'});
}

/**
 * Waits for an email to arrive for a specific recipient.
 * @param email - The recipient email address
 * @param timeoutMs - Maximum time to wait in milliseconds (default: 10000)
 * @param pollIntervalMs - How often to check for the email (default: 500)
 * @returns The email when it arrives
 * @throws Error if timeout is reached
 */
export async function waitForEmail(
    email: string,
    timeoutMs = 10000,
    pollIntervalMs = 500
): Promise<MailpitMessage> {
    const start = Date.now();

    while (Date.now() - start < timeoutMs) {
        const message = await getLatestEmailForRecipient(email);
        if (message) {
            return message;
        }
        await new Promise(resolve => setTimeout(resolve, pollIntervalMs));
    }

    throw new Error(`No email received for ${email} within ${timeoutMs}ms`);
}

/**
 * Waits for and extracts a password reset link from an email.
 * @param email - The recipient email address
 * @param timeoutMs - Maximum time to wait
 * @returns The password reset URL
 */
export async function getPasswordResetLink(email: string, timeoutMs = 10000): Promise<string> {
    await waitForEmail(email, timeoutMs);

    const message = await getLatestEmailForRecipient(email);
    if (!message) {
        throw new Error(`No email found for ${email}`);
    }

    const content = await getEmailContent(message.ID);
    const htmlContent = content.HTML || content.Text;

    // Extract href from the reset password link
    const hrefMatch = htmlContent.match(/href="([^"]*reset-password\?token=[^"]*)"/);
    if (hrefMatch) {
        return hrefMatch[1];
    }

    // Fallback: try to find the URL directly
    const urlMatch = htmlContent.match(/https?:\/\/[^\s"<>]*reset-password\?token=[^\s"<>]*/);
    if (urlMatch) {
        return urlMatch[0];
    }

    throw new Error('Password reset link not found in email');
}

/**
 * Waits for and extracts an email verification link from an email.
 * @param email - The recipient email address
 * @param timeoutMs - Maximum time to wait
 * @returns The email verification URL
 */
export async function getEmailVerificationLink(email: string, timeoutMs = 10000): Promise<string> {
    await waitForEmail(email, timeoutMs);

    const message = await getLatestEmailForRecipient(email);
    if (!message) {
        throw new Error(`No email found for ${email}`);
    }

    const content = await getEmailContent(message.ID);
    const htmlContent = content.HTML || content.Text;

    // Extract href from the verify email link
    const hrefMatch = htmlContent.match(/href="([^"]*verify-email\?token=[^"]*)"/);
    if (hrefMatch) {
        return hrefMatch[1];
    }

    // Fallback: try to find the URL directly
    const urlMatch = htmlContent.match(/https?:\/\/[^\s"<>]*verify-email\?token=[^\s"<>]*/);
    if (urlMatch) {
        return urlMatch[0];
    }

    throw new Error('Email verification link not found in email');
}

/**
 * Waits for and extracts a registration verification link from an email.
 * @param email - The recipient email address
 * @param timeoutMs - Maximum time to wait
 * @returns The registration verification URL
 */
export async function getRegistrationVerificationLink(email: string, timeoutMs = 10000): Promise<string> {
    await waitForEmail(email, timeoutMs);

    const message = await getLatestEmailForRecipient(email);
    if (!message) {
        throw new Error(`No email found for ${email}`);
    }

    const content = await getEmailContent(message.ID);
    const htmlContent = content.HTML || content.Text;

    // Extract href from the verify registration link
    const hrefMatch = htmlContent.match(/href="([^"]*verify-registration\?token=[^"]*)"/);
    if (hrefMatch) {
        return hrefMatch[1];
    }

    // Fallback: try to find the URL directly
    const urlMatch = htmlContent.match(/https?:\/\/[^\s"<>]*verify-registration\?token=[^\s"<>]*/);
    if (urlMatch) {
        return urlMatch[0];
    }

    throw new Error('Registration verification link not found in email');
}

/**
 * Gets the count of emails in Mailpit.
 * @returns The total number of emails
 */
export async function getEmailCount(): Promise<number> {
    const response = await fetch(`${MAILPIT_API}/messages`);
    if (!response.ok) {
        throw new Error(`Mailpit API error: ${response.status} ${response.statusText}`);
    }
    const data = await response.json();
    return data.total || 0;
}

/**
 * Waits for and extracts an invite link from an email.
 * @param email - The recipient email address
 * @param timeoutMs - Maximum time to wait
 * @returns The invite URL
 */
export async function getInviteLink(email: string, timeoutMs = 10000): Promise<string> {
    await waitForEmail(email, timeoutMs);

    const message = await getLatestEmailForRecipient(email);
    if (!message) {
        throw new Error(`No email found for ${email}`);
    }

    const content = await getEmailContent(message.ID);
    const htmlContent = content.HTML || content.Text;

    // Extract href from the invite link
    const hrefMatch = htmlContent.match(/href="([^"]*register\?invite=[^"]*)"/);
    if (hrefMatch) {
        return hrefMatch[1];
    }

    // Fallback: try to find the URL directly
    const urlMatch = htmlContent.match(/https?:\/\/[^\s"<>]*register\?invite=[^\s"<>]*/);
    if (urlMatch) {
        return urlMatch[0];
    }

    throw new Error('Invite link not found in email');
}
