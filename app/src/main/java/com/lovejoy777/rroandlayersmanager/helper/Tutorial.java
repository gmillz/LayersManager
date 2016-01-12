package com.lovejoy777.rroandlayersmanager.helper;

import android.app.Activity;

import com.lovejoy777.rroandlayersmanager.R;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;

import java.util.ArrayList;
import java.util.List;

public class Tutorial {

    public static void loadTutorial(final Activity context) {
        new IntroductionBuilder(context).withSlides(generateSlides()).introduceMyself();
    }

    public static List<Slide> generateSlides() {
        List<Slide> slides = new ArrayList<>();

        slides.add(new Slide()
                .withTitle(R.string.Slide1_Heading)
                .withDescription(R.string.Slide1_Text)
                .withColorResource(R.color.tutorial_background_1)
                .withImage(R.drawable.layersmanager));
        slides.add(new Slide()
                .withTitle(R.string.Slide2_Heading)
                .withDescription(R.string.Slide2_Text)
                .withColorResource(R.color.tutorial_background_2)
                .withImage(R.drawable.intro_2));
        slides.add(new Slide()
                .withTitle(R.string.Slide3_Heading)
                .withDescription(R.string.Slide3_Text)
                .withColorResource(R.color.tutorial_background_3)
                .withImage(R.drawable.intro_3));
        slides.add(new Slide()
                .withTitle(R.string.Slide4_Heading)
                .withDescription(R.string.Slide4_Text)
                .withColorResource(R.color.tutorial_background_4)
                .withImage(R.drawable.intro_4));
        slides.add(new Slide()
                .withTitle(R.string.Slide5_Heading)
                .withDescription(R.string.Slide5_Text)
                .withColorResource(R.color.tutorial_background_5)
                .withImage(R.drawable.intro_5));
        slides.add(new Slide()
                .withTitle(R.string.Slide6_Heading)
                .withOption(new Option(R.string.SettingLauncherIconDetail))
                .withColorResource(R.color.tutorial_background_6)
                .withImage(R.drawable.layersmanager_crossed));
        slides.add(new Slide()
                .withTitle(R.string.Slide7_Heading)
                .withOption(new Option(R.string.SettingsHideOverlays))
                .withColorResource(R.color.tutorial_background_6)
                .withImage(R.drawable.intro_7));
        return slides;
    }
}
