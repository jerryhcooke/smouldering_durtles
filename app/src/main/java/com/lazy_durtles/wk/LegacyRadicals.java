/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lazy_durtles.wk;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Store for names and mnemonics of the old (pre December 2018) radicals.
 */
@SuppressWarnings("LongLine")
public final class LegacyRadicals {
    private static final Map<Long, String> legacyNames = new HashMap<>();
    private static final Map<Long, String> legacyMnemonics = new HashMap<>();

    private LegacyRadicals() {
        //
    }

    private static void addLegacyRadical(final long id, final String name, final String mnemonic) {
        legacyNames.put(id, name);
        legacyMnemonics.put(id, mnemonic);
    }

    /**
     * Is the subject ID of a radical with an old name and mnemonic?.
     *
     * @param id the subject ID
     * @return true if it is
     */
    public static boolean isLegacyRadical(final long id) {
        return legacyNames.containsKey(id);
    }

    /**
     * The name of the given legacy radical.
     *
     * @param id the subject ID
     * @return the name or null if it doesn't have one
     */
    public static @Nullable String getLegacyName(final long id) {
        return legacyNames.get(id);
    }

    /**
     * The mnemonic of the given legacy radical.
     *
     * @param id the subject ID
     * @return the mnemonic or null if it doesn't have one
     */
    public static @Nullable String getLegacyMnemonic(final long id) {
        return legacyMnemonics.get(id);
    }

    static {
        addLegacyRadical(14, "Enclosure", "This radical encloses a lot of things (or at least tries to enclose things). It's like a big latchy box that goes around something and then chomps down to enclose it, making this the <radical>enclosure</radical> radical. Imagine something like this trying to come down from the sky to enclose you. Scary looking, right?");
        addLegacyRadical(17, "Grave", "There's a cross in the ground, which makes this a <radical>grave</radical>. Now, later on you'll learn a really similar radical with a cross that's wider than the ground. Just remember, this is a regular ol' grave, where the cross is not as wide as the ground.");
        addLegacyRadical(27, "Nail", "See the barb radical with a head on it? It looks just like a nail. That barb on the end looks painful. Imagine getting that nailed into your arm, with someone hitting the head of this nail with a big hammer. Imagine how much it would hurt to pull out, too. No, really, imagine it - it will help you to remember this radical so much better. Thwack Thwack Thwack!");
        addLegacyRadical(36, "Spring", "Boing, boing, boing. This looks just like a giant <radical>spring</radical> that you might put on the bottom of your shoes to jump higher.");
        addLegacyRadical(42, "Doll", "Doesn't this look like a <radical>doll</radical>? It has a head, arms, legs, and a body! So pretty.");
        addLegacyRadical(50, "Real", "With all the fake things in this world, it's sometimes nice to get something that comes out of the ground, like a tree. Things like this are <radical>real</radical>, not fake like plastic, etc.");
        addLegacyRadical(61, "Antenna", "Doesn't this look like the <radical>antenna</radical> that would be on top of a house or maybe on top of a television? That's why this radical means \"antenna.\"");
        addLegacyRadical(62, "Mullet", "It's a party in the back and business in the front. It's a <radical>mullet</radical>. You can see the short hair(s) on top, even, and the long, sweeping beauty in the back. This is the best haircut in the world. This is a mullet.");
        addLegacyRadical(63, "Nailbat", "This barb has two things sticking through it. This looks like a devastating weapon. Why, it looks like a <radical>nailbat</radical>, actually!");
        addLegacyRadical(71, "Elephant", "This radical is made up of two moon radicals stuck together. Two moons stuck together would be something huge - in this case, that huge thing is an <radical>elephant</radical>. In fact, if you look at the radical itself, it looks like the profile of an elephant, with its trunk on the right side and its legs in the middle and back!");
        addLegacyRadical(78, "Helmet", "The <radical>helmet</radical> is almost like the lid (亠) except it has the little side things to hold it onto something. That's why this is a helmet that goes onto things you need to protect.");
        addLegacyRadical(80, "Pelican", "This radical kind of looks like a <radical>pelican</radical> standing there with its giant pelican mouth open. It even has the long legs, too.");
        addLegacyRadical(81, "Frog", "Do you remember / know of the game \"Frogger\"? Looking from a bird's eye view, this radical looks just like a <radical>frog</radical>, running across the highway.");
        addLegacyRadical(83, "Generation", "If you look at this radical, you'll notice that the inner piece built onto the ground radical and the outer piece, also built onto the ground are kind of the same... except, the outer piece isn't completely finished yet. The middle bit is all done, and they're the center. The second layer currently being formed is the next <radical>generation</radical>. Surely, there will be even another generation going around the second layer (when it's done) and another and another later on.");
        addLegacyRadical(94, "Clam", "There's a giant eye on top of some fins. Imagine this giant eye to be on top of a <radical>clam</radical>. It has little fins on which it runs around on, escaping from your every attempt to capture it.");
        addLegacyRadical(102, "Two Face", "You have a head, and a line going right through the middle. Who else has a line going through the middle of their face? None other than the Batman villain <radical>Two Face</radical>.");
        addLegacyRadical(114, "Water Slide", "You have a drop of water at the top, and then a squiggly looking thing that looks like it could be a slide. That's why this radical is the <radical>water slide</radical>. What fun!");
        addLegacyRadical(125, "Sunflower", "See the big flower at the top of the stem and ground? It's the biggest flower of them all, a bright and beautiful <radical>sunflower</radical>.");
        addLegacyRadical(128, "Ribs", "Not only does this radical look like the hiragana/katakana character リ (which is the first two letters of the word \"ribs\"), but this radical also kind of looks like <radical>ribs</radical> itself, though you'll have to turn them sideways in your mind, first.");
        addLegacyRadical(129, "Chopsticks", "There's two <radical>chopsticks</radical> grabbing on to some rice. Of course, the person using them isn't all that skilled, but we'll forgive them, for now...");
        addLegacyRadical(131, "Fish Tail", "You know what fins are - double that and you have a <radical>fish tail</radical>.");
        addLegacyRadical(132, "Bad", "The evening radical is under the ground. Things that happen underground at night are usually all <radical>bad</radical> things. Think of all the bad underground stuff that happens at night. Gambling, prostitution, drug dealing... you name it. It's all bad.");
        addLegacyRadical(135, "Pine", "This radical is a tree with something extra on the top. Although it's sort of arbitrary, we're going to associate this kind of tree with a <radical>pine</radical> tree, making this radical a pine.");
        addLegacyRadical(140, "Pile", "This little triangle isn't quite a triangle because it's not one single thing. It's several things put together to form a little <radical>pile</radical> of something. ");
        addLegacyRadical(147, "Duck", "If you look at this radical the right way, you can see a <radical>duck</radical> head, facing to the right. What a majestic duck this is, too.");
        addLegacyRadical(148, "Soldier", "This radical is a dude standing up straight, swinging his arms with his march, and carrying a sword (which hangs off to the left). That's why this is the <radical>soldier</radical> radical.");
        addLegacyRadical(149, "Sail", "This looks like some billowing <radical>sail</radical>s being puffed up by the wind, which is why this radical is \"sail.\"");
        addLegacyRadical(159, "Boobs", "This radical consists of two piles. Whether you're a dude or a gal, you probably have two piles on your chest. That's why these are <radical>boobs</radical>.");
        addLegacyRadical(160, "Ikea", "This radical has both table and stool in it, shrunken down into a small space. Who else does that (and also has tables and stools?)? It would have to be the incredible furniture company <radical>Ikea</radical>!");
        addLegacyRadical(162, "Waiter", "The <radical>waiter</radical> is walking towards you with a dish in his hand (you see it in his outstretched arm?). That's how you know he's a waiter, he's bringing you your food.");
        addLegacyRadical(163, "Sauron", "This is a sideways eye. Whose big eye is this? It's <radical>Sauron</radical>'s eye. He's looking for those hobbits he is.");
        addLegacyRadical(166, "Hut", "See the roof on top of this little building? It's like a little <radical>hut</radical> you might see next to a beach, or something!");
        addLegacyRadical(169, "Ghost", "See the head radical wisping off to the left side? Also, do you see the mouth, and the strange face? It's moaning and floating around. Only one thing does this, and it is a <radical>ghost</radical>.");
        addLegacyRadical(172, "Gravity", "The sun has a wing on it. That wing symbolizes the sun spinning around, pulling the planets in. That's the <radical>gravity</radical> of the sun, so remember this radical as \"gravity\".");
        addLegacyRadical(176, "Butcher", "This radical looks like the letter \"B\" - what does \"B\" stand for? In this case, it stands for <radical>butcher</radical>.");
        addLegacyRadical(183, "Superman", "Who's strong enough to kick someone inside of a cliff then under the ground? The only person I can think of that strong is <radical>superman</radical>.");
        addLegacyRadical(190, "Say Humbly", "This radical is the same as the kanji you learned earlier, 申. They have the same meanings too. This radical means <radical>say humbly</radical>.");
        addLegacyRadical(196, "Good", "You wouldn't think so, but kicking the sun is a really <radical>good</radical> thing. If you don't kick it every once in a while, it won't come up. So, don't forget that it's good to kick the sun, okay?");
        addLegacyRadical(199, "Witch Doctor", "Doesn't this radical look like a <radical>witch doctor</radical> standing there with a mask and feathers / other things in his hair?");
        addLegacyRadical(204, "Tie Fighter", "This radical looks just like a <radical>tie fighter</radical> as seen from above / below / the front. Vrroozooomm!");
        addLegacyRadical(206, "Big Bird", "This looks like something that's really big taking huge steps. It also looks like it has a beak. What's big and has a beak? The famous character <radical>Big Bird</radical>.");
        addLegacyRadical(209, "Injustice", "No matter which way you look, there's spikes everywhere. You're being punished for something you didn't do, and that punishment is to be put in a room covered in spikes. You have suffered quite the <radical>injustice</radical>.");
        addLegacyRadical(218, "Devil", "You've probably seen how the identical kanji <ja>反</ja> means <kanji>anti</kanji>. I want you to have a better radical for that, though, since it comes up a lot. When you think of the word \"anti\" what do you think of? How about the Anti-Christ? Who does he work for? That's the <radical>devil</radical>.");
        addLegacyRadical(219, "Ent", "There is a tree with a mouth on it. In terms of trees with mouths, I think Tree <radical>Ent</radical>s make the most sense. They're the big tree people from the Lord Of The Rings trilogy. Go watch those movies / read those books and come back if you need to know more.");
        addLegacyRadical(226, "Books", "If you put a bunch of <radical>books</radical> into a shelf, they'd kind of look like this, no?");
        addLegacyRadical(228, "Cobra", "This looks a lot like the snake you will learn later on, but it's different because this snake is standing up, ready to strike. Can you think of a snake that takes up this position? How about a <radical>cobra</radical>?");
        addLegacyRadical(230, "Diamond", "Something small and white is inside a cliff. Technically they're clear, but when I think of small white things in cliffs or mountains I think of diamonds. That's why this radical is <radical>diamond</radical>.");
        addLegacyRadical(231, "Central", "This radical looks just like the kanji <kanji>central</kanji>. So, we're going to name this radical <radical>central</radical> as well.");
        addLegacyRadical(232, "Fish Stick", "This <radical>stick</radical> has fins on it. Why? Because it's a <radical>fish stick</radical>.");
        addLegacyRadical(234, "Somebody", "This is the same as the kanji <kanji>somebody</kanji>. That's why we're calling this radical <radical>somebody</radical> as well.");
        addLegacyRadical(244, "Cemetery", "Here is a grave pile. Where do grave piles (usually) exist? In a <radical>cemetery</radical>.");
        addLegacyRadical(248, "Glue", "This radical is the same as the kanji <kanji>attach</kanji>. Because the word \"attach\" is so terrible when it comes to creating mnemonics, let's change it to mean <radical>glue</radical>. After all, glue attaches one thing to another thing, so I think it's pretty appropriate.");
        addLegacyRadical(250, "Robot", "This radical looks like a <radical>robot</radical> head. It has an antenna on top, plus a strange face which is made up of several sensors all in a row. That's why we're calling this one \"robot.\"");
        addLegacyRadical(253, "Longcat", "Something comes from a samurai's mouth. Totally random, but that thing is a <radical>longcat</radical> which is basically a really, really long cat. Imagine it coming out of the samurai's mouth forever and ever. It never stops! So, when you think of a samurai's mouth, be sure to think of longcat, because it's coming out.");
        addLegacyRadical(273, "Arrows", "There are three arrows pointing to the left. Instead of one arrow (like with the radical and kanji <ja>矢</ja>), these are three arrows... aka plural, more than one! <reading>Arrows</reading>!");
        addLegacyRadical(274, "Boob Grave", "You have a boob and a grave. So, naturally this is a <radical>boob grave</radical>. Just imagine a single boob being buried, getting a service, and having people crying all around.");
        addLegacyRadical(277, "Stilts", "See the guy on top walking on those two long sticks? Those sticks are <radical>stilts</radical>.");
        addLegacyRadical(284, "Slinky", "Not only is it one spring, it is twenty springs. Combine these springs together to form a mega spring, an amazing <radical>slinky</radical>.");
        addLegacyRadical(285, "Outhouse", "This is an <radical>outhouse</radical>. There's a lid over the poop to keep the stink out, but for some reason you're dangling your legs into the outhouse hole. You'll have to come up with a reason why, but just try to imagine this and gross yourself out - it will help you to remember.");
        addLegacyRadical(289, "Four", "This radical looks just like a number <radical>four</radical>. It's just the version of <radical>four</radical> that isn't connected at the top.");
        addLegacyRadical(290, "Nailgun", "You have a lid covering your face, from your forehead to your mouth. There's a nail sticking in it. The reason you have this lid up protecting you like this is because someone's trying to use a <radical>nailgun</radical> to shoot you in the face.");
        addLegacyRadical(293, "Sunrise", "The sun is coming up out of the ground, making this radical <radical>sunrise</radical>. Of course, don't confuse this with sunset! You can do that by thinking that the sun is an optimistic and bright thing, so it tends to come up, not go down (at least whenever possible).");
        addLegacyRadical(294, "Cow God", "The five is a bit altered, but you can see it. So, you have five mouths. And you have a cow. In this case, you have a five mouthed cow. This is the <radical>cow god</radical>.");
        addLegacyRadical(303, "Circus", "You put your life (twice, one for each show) into an elephant's hands (or trunk). Where do you work? At the <radical>circus</radical>. Don't get this confused with the similar <ja>黄</ja>, though. That one is different, and doesn't involve life or elephants.");
        addLegacyRadical(315, "Thorn", "This radical looks like a (crazy) hand with something sticking right through it. What do you usually get stuck in your finger? That little thing is a <radical>thorn</radical>.");
        addLegacyRadical(319, "Ox", "What has big horns but a small sun? Let's say it's an <reading>ox</reading>, because there's Babe the giant blue ox, and oxes are pretty big anyways. And, if an ox is big enough, it makes the sun look small in comparison, especially if you're Babe the giant blue ox (though you only have to remember \"ox\").");
        addLegacyRadical(331, "Joker", "Taking a sword to a mouth... sounds like something the <radical>Joker</radical> from Batman would do. He likes to carve smiles into people's faces, which is why this radical means \"Joker.\"");
        addLegacyRadical(336, "Chester", "He's a creeper and he's \"Four-t\" (forty). He's known as <radical>Chester</radical> the molester. Don't cross paths with this sicko.");
        addLegacyRadical(338, "Bookshelf", "There's a door on the books. The door is part of a <radical>bookshelf</radical>, which holds the books together.");
        addLegacyRadical(367, "Propaganda", "A national flag in your mouth was promised to taste sweet but it is actually spicy. You've been lied to. These lies are <radical>propaganda</radical>.");
        addLegacyRadical(388, "Redwood", "This is a tree with two extra branches on top. It's the tallest of the tree-like radicals, which is why we're making this one the <radical>redwood</radical>, because redwoods are super tall and big and live a long time.");
        addLegacyRadical(428, "Kiln", "This radical is a jar on top of some kind of contraption. Well that contraption is actually a big furnace and the jar is a clay pot, making this whole thing a kiln. A kiln is like a big oven you use to harden pottery. So this radical is a <radical>kiln</radical>.");
        addLegacyRadical(429, "Protester", "This radical is made up of evening and cow. In the evening cows are let free by protesters who don't think it's right to keep them caged! They only do it at night, because they'd get arrested if they tried during the day. That's why this radical is <radical>protester</radical>.");
        addLegacyRadical(431, "Demolition Man", "This radical is made up of two face, explosion, and winter. Two-face likes to work a job where he gets to cause lots of explosions in winter, when the criming gets hard. So in winter he becomes a demolition man. That's why this radical is a <radical>demolition man</radical>, just like him.");
        addLegacyRadical(8776, "Boob", "Before you had two piles. Now you only have one. One pile is singular, that's why this pile is just a single <radical>boob</radical>. Also, it has a little nipple on there, making it more boob-like, whether it's a dude-boob or a lady-boob, that's up to your own imagination.");
        addLegacyRadical(8779, "Train", "This radical looks like a railway track going off into the distance. What rides on railway tracks? That would be a <radical>train</radical>.");
        addLegacyRadical(8780, "Cloak", "The <radical>cloak</radical> goes over everything. It protects it and keeps hidden whatever is below it. This looks like a cloak too, right? The right side is the cape part, coming down, and the top part is the part you tie around your neck.");
        addLegacyRadical(8787, "Hick", "There's a mullet here with a little head in it. Finally, someone is wearing the mullet. Who wears mullets, typically? That would be the <radical>hick</radical>.");
        addLegacyRadical(8794, "Shark", "This looks like it is water, but it's barbed and sharp. It also has a fish tail. What is in the water, has \"barbs\" and has a fish tail? Something that will bite you, that's for sure. Let's call this radical <radical>shark</radical>.");
    }
}
