package org.carrot2.labs.smartsprites;

import static org.carrot2.labs.test.Assertions.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.carrot2.labs.smartsprites.SmartSpritesParameters.PngDepth;
import org.carrot2.labs.smartsprites.message.Message;
import org.carrot2.labs.smartsprites.message.Message.MessageLevel;
import org.junit.*;

/**
 * Test cases for {@link SpriteBuilder}. The test cases read/ write files to the
 * directories contained in the test/ directory.
 */
public class SpriteBuilderTest extends TestWithMemoryMessageSink
{
    private SpriteBuilder spriteBuilder;

    @Test
    public void testNoSpriteDeclarations() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("no-sprite-declarations");

        buildSprites(testDir);

        assertThat(processedCss()).doesNotExist();
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testNoSpriteReferences() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("no-sprite-references");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testTargetSpriteImageDirNotExists() throws FileNotFoundException,
        IOException
    {
        final File testDir = testDir("target-sprite-image-dir-not-exists");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img-sprite/sprite.png")).exists();
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);

        FileUtils.deleteDirectory(new File(testDir, "img-sprite"));
    }

    @Test
    public void testSimpleHorizontalSprite() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("simple-horizontal-sprite");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(17 + 15 + 48, 47));
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testLargeVerticalRepeat() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("large-vertical-repeat");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(17 + 15, 16 * 17 /* lcm(16, 17) */));
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testMissingImages() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("missing-images");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(18, 17 + 6 + 5));

        // The unsatisfied sprite references are not removed from the output
        // file, hence we have two warnings
        assertThat(messages).isEquivalentTo(
            Message.MessageLevel.WARN,
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.CANNOT_NOT_LOAD_IMAGE, new File(testDir,
                    "css/style-expected.css").getCanonicalPath(), 15, new File(testDir,
                    "img/logo.png").getCanonicalPath(), "Can't read input file!"),
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.CANNOT_NOT_LOAD_IMAGE, new File(testDir,
                    "css/style.css").getCanonicalPath(), 15, new File(testDir,
                    "img/logo.png").getCanonicalPath(), "Can't read input file!"));
    }

    @Test
    public void testUnsupportedSpriteProperties() throws FileNotFoundException,
        IOException
    {
        final File testDir = testDir("unsupported-sprite-properties");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(48, 16 + 17 + 47));

        final String styleCssPath = new File(testDir, "css/style.css").getCanonicalPath();
        assertThat(messages).isEquivalentTo(
            Message.MessageLevel.WARN,
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.UNSUPPORTED_PROPERTIES_FOUND, styleCssPath, 4,
                "sprites-layout"),
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.UNSUPPORTED_PROPERTIES_FOUND, styleCssPath, 14,
                "sprites-margin-top"),
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.UNSUPPORTED_PROPERTIES_FOUND, styleCssPath, 18,
                "sprites-alignment"));
    }

    @Test
    public void testOverridingCssProperties() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("overriding-css-properties");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(17 + 15 + 48, 47));

        final String styleCssPath = new File(testDir, "css/style.css").getCanonicalPath();
        assertThat(messages).isEquivalentTo(
            Message.MessageLevel.WARN,
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.OVERRIDING_PROPERTY_FOUND, styleCssPath, 10,
                "background-image", 9),
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.OVERRIDING_PROPERTY_FOUND, styleCssPath, 21,
                "background-position", 20));
    }

    @Test
    public void testAbsoluteImageUrl() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("absolute-image-url");
        final File documentRootDir = testDir("absolute-image-url/absolute-path");
        buildSprites(new SmartSpritesParameters(testDir, null, documentRootDir,
            MessageLevel.INFO, SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
            SmartSpritesParameters.DEFAULT_CSS_INDENT,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_DEPTH,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6));

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        final File spriteFile = new File(documentRootDir, "img/sprite.png");
        assertThat(spriteFile).exists();
        org.fest.assertions.Assertions.assertThat(ImageIO.read(spriteFile)).hasSize(
            new Dimension(17, 17));
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);

        spriteFile.delete();
    }

    @Test
    public void testNonDefaultOutputDir() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("non-default-output-dir");
        final File documentRootDir = testDir("non-default-output-dir/absolute-path");
        final File outputDir = testDir("non-default-output-dir/output-dir");
        outputDir.mkdirs();
        buildSprites(new SmartSpritesParameters(testDir, outputDir, documentRootDir,
            MessageLevel.INFO, SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
            SmartSpritesParameters.DEFAULT_CSS_INDENT,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_DEPTH,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6));

        assertThat(expectedCss()).hasSameContentAs(processedCss());

        final File absoluteSpriteFile = new File(documentRootDir, "img/absolute.png");
        assertThat(absoluteSpriteFile).exists();
        org.fest.assertions.Assertions.assertThat(ImageIO.read(absoluteSpriteFile))
            .hasSize(new Dimension(17, 17));

        final File relativeSpriteFile = new File(outputDir, "img/relative.png");
        assertThat(relativeSpriteFile).exists();
        org.fest.assertions.Assertions.assertThat(ImageIO.read(relativeSpriteFile))
            .hasSize(new Dimension(15, 16));
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);

        FileUtils.deleteDirectory(outputDir);
        FileUtils.deleteDirectory(absoluteSpriteFile.getParentFile());
    }

    /**
     * Currently multiple references to the same image generate multiple copies of the
     * image in the sprite, hence the test is ignored.
     */
    @Test
    @Ignore
    public void testRepeatedImageReferences() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("repeated-image-references");
        buildSprites(testDir);

        assertThat(expectedCss()).hasSameContentAs(processedCss());
        assertThat(new File(testDir, "img/sprite.png")).exists();
        org.fest.assertions.Assertions.assertThat(sprite(testDir)).hasSize(
            new Dimension(17, 17));
        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testIndexedColor() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("indexed-color");
        buildSprites(testDir);

        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.gif")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.png")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-full-alpha.png")).isDirectColor().hasTrueAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-many-colors.png")).isDirectColor()
            .doesNotHaveAlpha();

        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testIndexedForcedDirectColor() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("indexed-color");
        buildSprites(new SmartSpritesParameters(testDir, null, null, MessageLevel.INFO,
            SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
            SmartSpritesParameters.DEFAULT_CSS_INDENT, PngDepth.DIRECT,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6));

        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.gif")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.png")).isDirectColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-full-alpha.png")).isDirectColor().hasTrueAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-many-colors.png")).isDirectColor()
            .doesNotHaveAlpha();

        assertThat(messages).doesNotHaveMessagesOfLevel(MessageLevel.WARN);
    }

    @Test
    public void testIndexedForcedIndexedColor() throws FileNotFoundException, IOException
    {
        final File testDir = testDir("indexed-color");
        buildSprites(new SmartSpritesParameters(testDir, null, null, MessageLevel.INFO,
            SmartSpritesParameters.DEFAULT_CSS_FILE_SUFFIX,
            SmartSpritesParameters.DEFAULT_CSS_INDENT, PngDepth.INDEXED,
            SmartSpritesParameters.DEFAULT_SPRITE_PNG_IE6));

        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.gif")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-bit-alpha.png")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-full-alpha.png")).isIndexedColor().hasBitAlpha();
        org.carrot2.labs.test.Assertions.assertThat(
            sprite(testDir, "img/sprite-many-colors.png")).isIndexedColor()
            .doesNotHaveAlpha();

        assertThat(messages).isEquivalentTo(
            Message.MessageLevel.WARN,
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.ALPHA_CHANNEL_LOSS_IN_INDEXED_COLOR, null, 25,
                "full-alpha"),
            new Message(Message.MessageLevel.WARN,
                Message.MessageType.TOO_MANY_COLORS_FOR_INDEXED_COLOR, null, 32,
                "many-colors", 293, 255));
    }

    @After
    public void cleanUp()
    {
        try
        {
            processedCss().delete();
        }
        catch (IllegalArgumentException e)
        {
            // We may get an IllegalArgumentException when the non-default output
            // dir gets deleted after tests. We can safely ignore that. An alternative
            // would be calling cleanUp manually in each test except those that
            // use non-default output directory.
        }

        // Delete sprites
        final File [] files = new File(spriteBuilder.parameters.rootDir, "img")
            .listFiles(new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return name.startsWith("sprite");
                }
            });
        if (files != null)
        {
            for (File file : files)
            {
                file.delete();
            }
        }
    }

    private File testDir(String test)
    {
        final File testDir = new File("test/" + test);
        return testDir;
    }

    private BufferedImage sprite(final File testDir) throws IOException
    {
        return sprite(testDir, "img/sprite.png");
    }

    private BufferedImage sprite(final File testDir, String imagePath) throws IOException
    {
        return ImageIO.read(new File(testDir, imagePath));
    }

    private File expectedCss()
    {
        return new File(spriteBuilder.parameters.rootDir, "css/style-expected.css");
    }

    private File sourceCss()
    {
        return new File(spriteBuilder.parameters.rootDir, "css/style.css");
    }

    private File processedCss()
    {
        return spriteBuilder.getProcessedCssFile(sourceCss());
    }

    private void buildSprites(File dir) throws IOException
    {
        buildSprites(new SmartSpritesParameters(dir));
    }

    private void buildSprites(SmartSpritesParameters parameters) throws IOException
    {
        spriteBuilder = new SpriteBuilder(parameters, messageLog);
        spriteBuilder.buildSprites();
    }
}