package ro.oks.quickjob.auth;

/**
 * Interface d'envoi de SMS. Implémentée par un fournisseur réel en production.
 */
public interface SmsSender {

    void send(String phoneNumber, String message);
}
