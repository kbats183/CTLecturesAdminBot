
package ru.kbats.youtube.broadcastscheduler.previewmaker;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SVGConvert {
    /*
    How to fix svg.

    remove patternContentUnits="objectBoundingBox" in pattern.
    move image to pattern tag
     */

    public static void main(String[] args) {
        var pngTranscoder = new PNGTranscoder();

        pngTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                SVGDOMImplementation.SVG_NAMESPACE_URI);

//        var svgURI = new File("test_text.svg").toURI();
        var svgURI = new File("preview/y2020.M32367.MathAn.svg").toURI();

//        SVGDocument doc = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName()).createSVGDocument(svgURI.toString());

//        TranscoderInput input = new TranscoderInput(doc);
        TranscoderInput input = new TranscoderInput(svgURI.toString());

        try (OutputStream ostream = new FileOutputStream("out.png")) {
            TranscoderOutput output = new TranscoderOutput(ostream);

            pngTranscoder.transcode(input, output);
        } catch (IOException e) {
            System.err.println("Can't write png file: " + e.getMessage());
        } catch (TranscoderException e) {
            System.err.println("Can't convert svg to png: " + e.getMessage());
        }
    }
}
