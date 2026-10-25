package io.github.niilo411.elementalpaths.platform.event;

/** NeoForge's ICancellableEvent, as far as this mod ever used it. */
public abstract class CancellableEvent {
    private boolean canceled;

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
