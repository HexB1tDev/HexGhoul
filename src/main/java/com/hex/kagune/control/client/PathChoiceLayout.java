package com.hex.kagune.control.client;

import com.hex.kagune.control.KagunePath;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometry of the path choice screen in GUI-scaled pixels. Pure (no Minecraft types), so every supported GUI
 * size can be tested: Minecraft never scales the GUI below 320x240.
 *
 * <p>Wide screens show the path cards side by side (one per choosable kagune); when a side-by-side card would be
 * narrower than {@link #MIN_SIDE_CARD_WIDTH} (below {@link #STACK_BELOW_WIDTH} for two paths) they stack and each
 * card switches to a short "silhouette left, text right" layout. Short screens drop one subtitle line and the key
 * hint.
 */
public final class PathChoiceLayout {
    public static final int MARGIN = 16;
    public static final int GAP = 12;
    /** 6.1.0: three or more stacked cards sit closer, so each keeps its height on the smallest screens. */
    public static final int STACK_GAP_MANY = 6;
    /** With two paths: narrower screens stack the cards. */
    public static final int STACK_BELOW_WIDTH = 420;
    /** Side-by-side cards are never narrower than this (two cards at {@link #STACK_BELOW_WIDTH}). */
    public static final int MIN_SIDE_CARD_WIDTH = 188;
    public static final int COMPACT_BELOW_HEIGHT = 300;
    public static final int MAX_CARD_WIDTH = 230;
    public static final int MAX_STACKED_CARD_WIDTH = 340;
    public static final int MAX_CARD_HEIGHT = 236;
    public static final int MAX_CONFIRM_WIDTH = 240;
    public static final int BUTTON_HEIGHT = 20;
    public static final int LATER_HEIGHT = 12;
    public static final int LINE = 10;
    public static final int TITLE_HEIGHT = 14;

    /** An axis-aligned rectangle; {@code contains} is half-open so neighbouring boxes never both claim a pixel. */
    public record Box(int x, int y, int width, int height) {
        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        public int centerX() {
            return this.x + this.width / 2;
        }

        public boolean contains(double px, double py) {
            return px >= this.x && px < this.right() && py >= this.y && py < this.bottom();
        }

        public boolean overlaps(Box other) {
            return this.x < other.right() && other.x < this.right() && this.y < other.bottom() && other.y < this.bottom();
        }

        public boolean inside(Box outer) {
            return this.x >= outer.x && this.y >= outer.y && this.right() <= outer.right() && this.bottom() <= outer.bottom();
        }
    }

    /**
     * Where things go inside one card. {@code stripY} and {@code tagsY} are -1 when there is no room for them.
     * Bullets start at {@code bulletsY}, one per {@link #LINE}, at most {@code maxBullets}.
     */
    public record Card(Box silhouette, int textX, int textWidth, int nameY, int typeY, int bulletsY, int maxBullets,
                       int tagsY, int stripY) {
    }

    public final int width;
    public final int height;
    public final boolean stacked;
    public final boolean compact;
    public final int titleY;
    public final int subtitleY;
    public final int subtitleLines;
    /** One card per choosable path, in starting-screen order. */
    public final List<Box> cards;
    public final Box confirm;
    public final Box later;
    /** Top of the key hint line, or -1 when the screen is too short for it. */
    public final int hintY;

    private PathChoiceLayout(int width, int height, int count) {
        int n = Math.max(1, count);
        this.width = width;
        this.height = height;
        this.stacked = (width - 2 * MARGIN - (n - 1) * GAP) / n < MIN_SIDE_CARD_WIDTH;
        this.compact = height < COMPACT_BELOW_HEIGHT;
        int top = this.compact ? 6 : 10;
        int breathing = this.compact ? 6 : 10;
        this.subtitleLines = this.compact ? 1 : 2;
        this.titleY = top;
        this.subtitleY = this.titleY + TITLE_HEIGHT + 4;
        int cardsTop = this.subtitleY + this.subtitleLines * LINE + breathing;

        int bottomPad = this.compact ? 4 : 8;
        this.hintY = this.compact ? -1 : height - bottomPad - LINE;
        int laterY = (this.hintY < 0 ? height - bottomPad : this.hintY - 2) - LATER_HEIGHT;
        int confirmY = laterY - 4 - BUTTON_HEIGHT;
        int cardsBottom = confirmY - breathing;

        int confirmWidth = Math.min(MAX_CONFIRM_WIDTH, width - 2 * MARGIN);
        this.confirm = new Box((width - confirmWidth) / 2, confirmY, confirmWidth, BUTTON_HEIGHT);
        int laterWidth = 90;
        this.later = new Box((width - laterWidth) / 2, laterY, laterWidth, LATER_HEIGHT);

        int area = Math.max(0, cardsBottom - cardsTop);
        List<Box> cards = new ArrayList<>();
        if (this.stacked) {
            int gap = n >= 3 ? STACK_GAP_MANY : GAP;
            int cardWidth = Math.min(MAX_STACKED_CARD_WIDTH, width - 2 * MARGIN);
            int cardHeight = Math.min(MAX_CARD_HEIGHT / 2, (area - (n - 1) * gap) / n);
            int x = (width - cardWidth) / 2;
            int y = cardsTop + (area - (n * cardHeight + (n - 1) * gap)) / 2;
            for (int i = 0; i < n; i++) {
                cards.add(new Box(x, y + i * (cardHeight + gap), cardWidth, cardHeight));
            }
        } else {
            int cardWidth = Math.min(MAX_CARD_WIDTH, (width - 2 * MARGIN - (n - 1) * GAP) / n);
            int cardHeight = Math.min(MAX_CARD_HEIGHT, area);
            int x = (width - (n * cardWidth + (n - 1) * GAP)) / 2;
            int y = cardsTop + (area - cardHeight) / 2;
            for (int i = 0; i < n; i++) {
                cards.add(new Box(x + i * (cardWidth + GAP), y, cardWidth, cardHeight));
            }
        }
        this.cards = List.copyOf(cards);
    }

    /** Screens narrower than this stack {@code count} cards (side by side each would be under {@link #MIN_SIDE_CARD_WIDTH}). */
    public static int stackBelowWidth(int count) {
        int n = Math.max(1, count);
        return n * MIN_SIDE_CARD_WIDTH + (n - 1) * GAP + 2 * MARGIN;
    }

    /** The layout for every choosable path. */
    public static PathChoiceLayout of(int width, int height) {
        return of(width, height, KagunePath.choosable().size());
    }

    public static PathChoiceLayout of(int width, int height, int count) {
        return new PathChoiceLayout(width, height, count);
    }

    /** Index of the card under a point, or -1. */
    public int cardAt(double x, double y) {
        for (int i = 0; i < this.cards.size(); i++) {
            if (this.cards.get(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    /** Inner layout for a card box produced by this layout. */
    public Card card(Box card) {
        int pad = 8;
        if (this.stacked) {
            // Wider than tall, so wings and tendrils have room to spread; the text column keeps at least 140 px.
            int size = Math.max(0, Math.min(card.height() - 12, 90));
            int silhouetteWidth = Math.max(size, Math.min(Math.round(size * 1.7F), card.width() - 12 - 8 - 8 - 140));
            Box silhouette = new Box(card.x() + 6, card.y() + 6, silhouetteWidth, size);
            int textX = silhouette.right() + 8;
            int textWidth = Math.max(0, card.right() - pad - textX);
            int nameY = card.y() + 6;
            int typeY = nameY + LINE;
            int bulletsY = typeY + LINE + 2;
            int stripY = card.height() >= 110 ? card.bottom() - 6 - 14 : -1;
            int limit = (stripY >= 0 ? stripY - 2 : card.bottom() - 4);
            return new Card(silhouette, textX, textWidth, nameY, typeY, bulletsY, Math.max(0, (limit - bulletsY) / LINE), -1, stripY);
        }
        int nameY = card.y() + pad;
        int typeY = nameY + LINE + 1;
        int silhouetteTop = typeY + LINE + 3;
        int silhouetteHeight = Math.max(24, Math.min(96, Math.round(card.height() * 0.38F)));
        Box silhouette = new Box(card.x() + pad, silhouetteTop, card.width() - 2 * pad, silhouetteHeight);
        int stripY = card.bottom() - pad - 14;
        int tagsY = stripY - 4 - LINE;
        int bulletsY = silhouette.bottom() + 6;
        int maxBullets = Math.max(0, (tagsY - 2 - bulletsY) / LINE);
        if (tagsY < bulletsY) {
            // Very short cards: the strip and tags give way to the silhouette and name.
            return new Card(silhouette, card.x() + pad, card.width() - 2 * pad, nameY, typeY, bulletsY, 0, -1, -1);
        }
        return new Card(silhouette, card.x() + pad, card.width() - 2 * pad, nameY, typeY, bulletsY, maxBullets, tagsY, stripY);
    }
}
