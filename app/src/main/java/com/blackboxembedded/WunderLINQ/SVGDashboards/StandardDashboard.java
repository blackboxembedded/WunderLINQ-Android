/*
WunderLINQ Client Application
Copyright (C) 2020  Keith Conger, Black Box Embedded, LLC

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
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

public class StandardDashboard {

    public final static String TAG = "standard-dashboard";
    private static Document templateDoc = null;
    private static String lastFilename = "";

    public static synchronized SVG updateDashboard(int infoLine){
        InputStream xml = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // Setup Help class to layout document
            SVGHelper h = new SVGHelper();
            String filename = SVGHelper.svgFilename(TAG);

            // Read/Cache SVG File template
            if (templateDoc == null || !filename.equals(lastFilename)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                templateDoc = builder.parse(MyApplication.getContext().getAssets().open(filename));
                lastFilename = filename;
            }

            // Create a new document and import the template's root element
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.appendChild(doc.importNode(templateDoc.getDocumentElement(), true));

            // Pre-cache elements for faster lookup
            h.preCacheElements(doc);

            //layout document using helper
            h.setupStandardLabels(doc);
            h.setupCustomData(doc, infoLine);
            h.setupSpeedo(doc);
            h.setupAmbientTemp(doc);
            h.setupEngineTemp(doc);
            h.setupClock(doc);
            h.setupRearRDC(doc);
            h.setupFrontRDC(doc);
            h.setupGear(doc);
            h.setupIcons(doc);
            h.setupRpmDialStandard(doc);
            h.setupTachStandard(doc);

            //Convert to document to XML and return
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource dSource = new DOMSource(doc);
            outputStream = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outputStream);
            transformer.transform(dSource, result);
            xml = new ByteArrayInputStream(outputStream.toByteArray());

            return SVG.getFromInputStream(xml);
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | NullPointerException e) {
            Log.d(TAG, "Exception updating dashboard: " + e.toString());
        } finally {
            // Close resources
            try {
                if (xml != null) xml.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "Exception updating dashboard: " + e.toString());
            }
        }
        return null;
    }
}
