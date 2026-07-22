package com.kiosk.util;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Анимации интерфейса по макету «Киоск — новый дизайн»:
 * screenIn / rowIn / sheetUp / dimIn / toastIn / barIn / pulseDot.
 * Тайминги и кривые повторяют CSS-ключевые кадры макета.
 */
public final class Fx {

    /** cubic-bezier(.2,.8,.2,1) — появление экранов и строк. */
    public static final Interpolator EASE_OUT = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);
    /** cubic-bezier(.2,.9,.25,1) — выезд шторки. */
    public static final Interpolator EASE_SHEET = Interpolator.SPLINE(0.2, 0.9, 0.25, 1);

    private Fx() {}

    /** screenIn: экран поднимается с 28px и проявляется, 450 мс. */
    public static void screenIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(28);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        TranslateTransition move = new TranslateTransition(Duration.millis(450), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        new ParallelTransition(fade, move).play();
    }

    /** rowIn: строка/карточка поднимается с 24px, задержка растёт по индексу. */
    public static void rowIn(Node node, int index) {
        node.setOpacity(0);
        node.setTranslateY(24);
        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setToValue(1);
        fade.setInterpolator(EASE_OUT);
        TranslateTransition move = new TranslateTransition(Duration.millis(400), node);
        move.setToY(0);
        move.setInterpolator(EASE_OUT);
        ParallelTransition p = new ParallelTransition(fade, move);
        // После ~12-й строки задержку не увеличиваем, чтобы длинные списки не «тянулись».
        p.setDelay(Duration.millis(Math.min(index, 12) * 45.0));
        p.play();
    }

    /** sheetUp: шторка выезжает снизу за 400 мс (высота берётся после раскладки). */
    public static void sheetUp(Node sheet) {
        sheet.setOpacity(0);
        Platform.runLater(() -> {
            double height = Math.max(sheet.getBoundsInParent().getHeight(), 400);
            sheet.setOpacity(1);
            sheet.setTranslateY(height);
            TranslateTransition move = new TranslateTransition(Duration.millis(400), sheet);
            move.setToY(0);
            move.setInterpolator(EASE_SHEET);
            move.play();
        });
    }

    /** Обратный ход шторки; по завершении выполняет onFinished (скрыть слой). */
    public static void sheetDown(Node sheet, Runnable onFinished) {
        double height = Math.max(sheet.getBoundsInParent().getHeight(), 400);
        TranslateTransition move = new TranslateTransition(Duration.millis(300), sheet);
        move.setToY(height);
        move.setInterpolator(Interpolator.EASE_IN);
        move.setOnFinished(e -> {
            sheet.setTranslateY(0);
            if (onFinished != null) {
                onFinished.run();
            }
        });
        move.play();
    }

    /** dimIn: затемнение позади шторки, 250 мс. */
    public static void dimIn(Node dim) {
        dim.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(250), dim);
        fade.setToValue(1);
        fade.play();
    }

    /** barIn: панель корзины выезжает снизу, 350 мс. */
    public static void barIn(Node bar) {
        Platform.runLater(() -> {
            double height = Math.max(bar.getBoundsInParent().getHeight() * 1.2, 60);
            bar.setTranslateY(height);
            TranslateTransition move = new TranslateTransition(Duration.millis(350), bar);
            move.setToY(0);
            move.setInterpolator(EASE_OUT);
            move.play();
        });
    }

    /**
     * toastIn: показывает тост (спуск с −16px + проявление), держит 2.2 с и прячет.
     * Возвращает анимацию, чтобы предыдущий тост можно было остановить.
     */
    public static Animation toast(Node toast) {
        toast.setVisible(true);
        toast.setOpacity(0);
        toast.setTranslateY(-16);

        FadeTransition in = new FadeTransition(Duration.millis(300), toast);
        in.setToValue(1);
        in.setInterpolator(EASE_OUT);
        TranslateTransition down = new TranslateTransition(Duration.millis(300), toast);
        down.setToY(0);
        down.setInterpolator(EASE_OUT);

        FadeTransition out = new FadeTransition(Duration.millis(220), toast);
        out.setToValue(0);

        SequentialTransition seq = new SequentialTransition(
                new ParallelTransition(in, down),
                new PauseTransition(Duration.millis(2200)),
                out);
        seq.setOnFinished(e -> toast.setVisible(false));
        seq.play();
        return seq;
    }

    /** pulseDot: точка статуса пульсирует (1 → 0.35 → 1), бесконечно. */
    public static Timeline pulse(Node dot) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(dot.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(700), new KeyValue(dot.opacityProperty(), 0.35)),
                new KeyFrame(Duration.millis(1400), new KeyValue(dot.opacityProperty(), 1)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        return timeline;
    }
}
