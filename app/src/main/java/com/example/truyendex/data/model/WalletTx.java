package com.example.truyendex.data.model;

/** A pending demo transfer. Each pack costs 10,000 VND for 30 coins. */
public final class WalletTx {
    public final String reference;
    public final int packs;

    public WalletTx(String reference, int packs) {
        if (packs < 1 || packs > 2) throw new IllegalArgumentException("Invalid pack");
        this.reference = reference;
        this.packs = packs;
    }

    public int amount() { return packs * 10000; }
    public int coins() { return packs * 30; }
}
