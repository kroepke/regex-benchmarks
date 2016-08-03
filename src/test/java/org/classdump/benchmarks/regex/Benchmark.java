package org.classdump.benchmarks.regex;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.joni.Option;
import org.joni.Regex;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Benchmark {


    private static final Collection<String> patterns = ImmutableList.of(
            "^(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)",
            // URL match
            "(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)",
            // URL match without starting ^
            "usd [+-]?[0-9]+.[0-9][0-9]",
            // Canonical US dollar amount
            "\\b(\\w+)(\\s+\\1)+\\b",
            // Duplicate words
            "\\{(\\d+):(([^}](?!-} ))*)"
            // this is meant to match against the "some more text and ..." but it causes ORO Matcher
            // to fail, so we won't include this by default... it is also WAY too slow to test
            // we will test large string 10 times
    );

    private static final Collection<String> strings = ImmutableList.of(
            "http://www.linux.com/",
            "http://www.thelinuxshow.com/main.php3",
            "usd 1234.00",
            "he said she said he said no",
            "same same same",
            "{1:\n" + "this is some more text - and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more\n"
                    + "this is some more text and some more and some more and even more at the end\n" + "-}\n"
            // very large bit of text...
    );

    private static boolean[][] expectedMatch = new boolean[patterns.size()][strings.size()];

    static {
        expectedMatch[0][0] = true;
        expectedMatch[0][1] = true;
        expectedMatch[0][2] = false;
        expectedMatch[0][3] = false;
        expectedMatch[0][4] = false;
        expectedMatch[0][5] = false;
        expectedMatch[1][0] = true;
        expectedMatch[1][1] = true;
        expectedMatch[1][2] = false;
        expectedMatch[1][3] = false;
        expectedMatch[1][4] = false;
        expectedMatch[1][5] = false;
        expectedMatch[2][0] = false;
        expectedMatch[2][1] = false;
        expectedMatch[2][2] = true;
        expectedMatch[2][3] = false;
        expectedMatch[2][4] = false;
        expectedMatch[2][5] = false;
        expectedMatch[3][0] = false;
        expectedMatch[3][1] = false;
        expectedMatch[3][2] = false;
        expectedMatch[3][3] = false;
        expectedMatch[3][4] = true;
        expectedMatch[3][5] = false;
        expectedMatch[4][0] = false;
        expectedMatch[4][1] = false;
        expectedMatch[4][2] = false;
        expectedMatch[4][3] = false;
        expectedMatch[4][4] = false;
        expectedMatch[4][5] = false;
    }


    @Test
    public void JavaUtilRegex() {
        final List<Pattern> p = patterns.stream().map(Pattern::compile).collect(Collectors.toList());


        // warm-up
        IntStream.range(0, 10).forEach(i -> {
            runJUP(p);
        });
        System.out.println("Warm up done.");
        final Stopwatch started = Stopwatch.createStarted();
        // warm-up
        IntStream.range(0, 100).forEach(i -> {
            runJUP(p);
        });
        final long micros = started.elapsed(TimeUnit.MICROSECONDS);
        System.out.println("JDK pattern: " + micros + "µs");

    }

    private void runJUP(List<Pattern> p) {
        final int[] patternNum = {0};
        p.forEach(pattern -> {
            final int[] stringNum = {0};

            strings.forEach(input -> {
                final Matcher matcher = pattern.matcher(input);
                final boolean b = matcher.find();
                if (b) {
//                System.out.println("Input " +  input + " matches " + pattern.pattern() + " : " + b);
                }


                stringNum[0]++;
            });
            patternNum[0]++;
        });
    }


    @Test
    public void Joni() {

        final List<Regex> p = patterns.stream().map(Regex::new).collect(Collectors.toList());


        // warm-up
        IntStream.range(0, 1).forEach(i -> {
            runJoni(p);
        });
        System.out.println("Warm up done.");
        final Stopwatch started = Stopwatch.createStarted();
        IntStream.range(0, 100).forEach(i -> {
            runJoni(p);
        });
        final long micros = started.elapsed(TimeUnit.MICROSECONDS);
        System.out.println("Joni pattern: " + micros + "µs");

    }

    private void runJoni(List<Regex> p) {
        final int[] patternNum = {0};
        p.forEach(pattern -> {
            final int[] stringNum = {0};

            strings.forEach(input -> {
                final byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                final org.joni.Matcher matcher = pattern.matcher(bytes);
                final int b = matcher.search(0, bytes.length, Option.CAPTURE_GROUP);
                if (b != -1) {
//                    System.out.println("Input " +  input + " matches " + pattern + " : " + b);
                }

                stringNum[0]++;
            });
            patternNum[0]++;
        });
    }

}
