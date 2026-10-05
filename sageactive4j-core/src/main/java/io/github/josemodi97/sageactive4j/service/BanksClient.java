package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.graphql.Pages;
import io.github.josemodi97.sageactive4j.input.ReconcileInput;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.BankAccount;
import io.github.josemodi97.sageactive4j.model.BankMovement;
import io.github.josemodi97.sageactive4j.model.PaymentMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Bank accounts, imported bank movements, reconciliation, payment methods. */
public final class BanksClient extends DomainClient {

    public BanksClient(SageActive4jClient client) {
        super(client);
    }

    /**
     * Bank accounts with balances and sync state. Sage Active documents this
     * as an unpaginated action; both a plain list and a connection are accepted.
     */
    @SuppressWarnings("unchecked")
    public List<BankAccount> bankAccounts() {
        Map<String, Object> data = organization("bankAccounts").query(GraphQLDocuments.operation("bankAccounts", null));
        Object value = data.get("bankAccounts");
        if (value instanceof Map) {
            return Connection.fromJson((Map<String, Object>) value, BankAccount::new).getNodes();
        }
        List<BankAccount> accounts = new ArrayList<BankAccount>();
        for (Object item : JsonReader.getList(data, "bankAccounts")) {
            if (item instanceof Map) {
                accounts.add(new BankAccount((Map<String, Object>) item));
            }
        }
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Movements imported from connected banks, newest first.
     *
     * @param bankAccountId only this account's movements; {@code null} = all connected accounts
     */
    public Connection<BankMovement> movements(String bankAccountId, ListOptions options) {
        if (bankAccountId == null) {
            return list(organization("bankMovements"), "bankMovements", options, null, "[{ datePosted: DESC }]", null,
                    BankMovement::new);
        }
        return list(organization("bankMovements"), "bankMovements", options,
                "{ bankAccountId: { eq: $bankAccountId } }", "[{ datePosted: DESC }]",
                GraphQLDocuments.var("bankAccountId", "UUID", bankAccountId), BankMovement::new);
    }

    public Iterable<BankMovement> allMovements(final String bankAccountId, ListOptions options) {
        return Pages.iterate(options, o -> movements(bankAccountId, o));
    }

    /** Links a movement to accounting entries; returns the movement id. */
    public String reconcile(ReconcileInput reconciliation) {
        Map<String, Object> data = organization("reconcileBankMovement").query(GraphQLDocuments.operation(
                "reconcileBankMovement", Collections.singletonMap("input", validated(reconciliation, "reconciliation"))));
        return JsonReader.getString(require(data, "reconcileBankMovement"), "id");
    }

    /** Removes a movement's reconciliation; returns the movement id. */
    public String unreconcile(String bankTransactionId) {
        Map<String, Object> data = organization("unReconcileBankMovement").query(GraphQLDocuments.operation(
                "unReconcileBankMovement",
                SalesClient.input("bankTransactionId", requireId(bankTransactionId, "bankTransactionId"))));
        return JsonReader.getString(require(data, "unReconcileBankMovement"), "id");
    }

    /** Bank accounts and cash registers as configured; their ids are settlement {@code paymentMethodId}s. */
    public Connection<PaymentMethod> paymentMethods(ListOptions options) {
        return list(organization("paymentMethods"), "paymentMethods", options, null, null, null, PaymentMethod::new);
    }
}
