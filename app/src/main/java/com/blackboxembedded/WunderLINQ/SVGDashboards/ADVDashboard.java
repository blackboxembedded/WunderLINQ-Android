package com.blackboxembedded.WunderLINQ.SVGDashboards;

import android.util.Log;

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.caverock.androidsvg.SVG;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ADVDashboard {
    private final static String TAG = "adv-dashboard";


    public static SVG updateDashboard(int infoLine){
        try {
            // Setup Help class to layout document
            SVGHelper h = new SVGHelper();

            // Read SVG File
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(MyApplication.getContext().getAssets().open(SVGHelper.svgFilename(TAG)));


            //layout document using helper
            h.setupCustomData(doc, infoLine);
            h.setupSpeedo(doc);
            h.setupClock(doc);
            h.setupGear(doc);
            h.setupIcons(doc);
            h.setupRpmDialStandard(doc);
            h.setupTachStandard(doc);
            h.setupCompass(doc);


            //Convert to document to XML and return
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(doc);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(dSource, result);
            InputStream xml = new ByteArrayInputStream(outputStream.toByteArray());

            return SVG.getFromInputStream(xml);
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | NullPointerException e) {
            Log.d(TAG, "Exception updating dashboard: " + e.toString());
        }
        return null;
    }
}
