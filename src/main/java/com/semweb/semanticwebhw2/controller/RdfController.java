package com.semweb.semanticwebhw2.controller;

import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Controller
public class RdfController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/upload-rdf")
    public String uploadRdfPage() {
        return "upload-rdf";
    }

    @PostMapping("/upload-rdf")
    public String uploadRdf(@RequestParam("file") MultipartFile file, Model springModel) {
        try {
            InputStream inputStream = file.getInputStream();
            org.apache.jena.rdf.model.Model rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(inputStream, null, "RDF/XML");

            List<Map<String, String>> triples = new ArrayList<>();

            StmtIterator iter = rdfModel.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                Map<String, String> triple = new HashMap<>();

                String subject = shorten(stmt.getSubject().toString());
                String predicate = shorten(stmt.getPredicate().toString());
                String object = shorten(stmt.getObject().toString());

                triple.put("subject", subject);
                triple.put("predicate", predicate);
                triple.put("object", object);
                triples.add(triple);
            }

            springModel.addAttribute("triples", triples);
            return "upload-rdf";

        } catch (Exception e) {
            springModel.addAttribute("error", "Failed to parse RDF file: " + e.getMessage());
            return "upload-rdf";
        }
    }

    private String shorten(String uri) {
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf("#") + 1);
        }
        return uri;
    }
}