package com.semweb.semanticwebhw2.service;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.util.*;

@Service
public class RdfService {

    private static final String NS_BOOK = "http://www.semweb.com/books#";
    private static final String NS_USER = "http://www.semweb.com/users#";
    private static final String RDF_FILE = "books.rdf";

    private org.apache.jena.rdf.model.Model loadModel() {
        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
        InputStream is = getClass().getClassLoader().getResourceAsStream(RDF_FILE);
        if (is != null) {
            model.read(is, null, "RDF/XML");
        }
        return model;
    }

    private void saveModel(org.apache.jena.rdf.model.Model model) {
        try {
            URL resource = getClass().getClassLoader().getResource(RDF_FILE);
            if (resource != null) {
                File file = new File(resource.toURI());
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    model.write(fos, "RDF/XML");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save RDF file: " + e.getMessage());
        }
    }

    public List<Map<String, String>> getAllBooks() {
        org.apache.jena.rdf.model.Model model = loadModel();
        List<Map<String, String>> books = new ArrayList<>();

        Property hasGenre = model.getProperty(NS_BOOK + "hasGenre");
        Property hasReadingLevel = model.getProperty(NS_BOOK + "hasReadingLevel");
        Property hasAuthor = model.getProperty(NS_BOOK + "hasAuthor");

        ResIterator iter = model.listSubjectsWithProperty(hasAuthor);
        while (iter.hasNext()) {
            Resource book = iter.next();
            Map<String, String> bookMap = new HashMap<>();

            String uri = book.getURI();
            String id = uri.contains("#") ? uri.substring(uri.lastIndexOf("#") + 1) : uri;
            bookMap.put("id", id);

            Statement labelStmt = book.getProperty(RDFS.label);
            bookMap.put("title", labelStmt != null ? labelStmt.getString() : id);

            Statement authorStmt = book.getProperty(hasAuthor);
            bookMap.put("author", authorStmt != null ? authorStmt.getString() : "Unknown");

            Statement levelStmt = book.getProperty(hasReadingLevel);
            if (levelStmt != null) {
                String levelUri = levelStmt.getObject().toString();
                bookMap.put("readingLevel", levelUri.contains("#") ?
                        levelUri.substring(levelUri.lastIndexOf("#") + 1) : levelUri);
            } else {
                bookMap.put("readingLevel", "Unknown");
            }

            StmtIterator genreIter = book.listProperties(hasGenre);
            List<String> genres = new ArrayList<>();
            while (genreIter.hasNext()) {
                String genreUri = genreIter.next().getObject().toString();
                genres.add(genreUri.contains("#") ?
                        genreUri.substring(genreUri.lastIndexOf("#") + 1) : genreUri);
            }
            bookMap.put("genres", String.join(", ", genres));

            books.add(bookMap);
        }
        return books;
    }

    public Map<String, String> getBookById(String id) {
        return getAllBooks().stream()
                .filter(b -> b.get("id").equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addBook(String id, String title, String author, String readingLevel, List<String> genres) {
        org.apache.jena.rdf.model.Model model = loadModel();

        Resource book = model.createResource(NS_BOOK + id);
        model.add(book, RDFS.label, title);
        model.add(book, model.getProperty(NS_BOOK + "hasAuthor"), author);
        model.add(book, model.getProperty(NS_BOOK + "hasReadingLevel"),
                model.getResource(NS_BOOK + readingLevel));

        for (String genre : genres) {
            model.add(book, model.getProperty(NS_BOOK + "hasGenre"),
                    model.getResource(NS_BOOK + genre));
        }

        saveModel(model);
    }

    public void updateBook(String id, String title, String author, String readingLevel, List<String> genres) {
        org.apache.jena.rdf.model.Model model = loadModel();

        Resource book = model.getResource(NS_BOOK + id);

        model.removeAll(book, RDFS.label, null);
        model.removeAll(book, model.getProperty(NS_BOOK + "hasAuthor"), null);
        model.removeAll(book, model.getProperty(NS_BOOK + "hasReadingLevel"), null);
        model.removeAll(book, model.getProperty(NS_BOOK + "hasGenre"), null);

        model.add(book, RDFS.label, title);
        model.add(book, model.getProperty(NS_BOOK + "hasAuthor"), author);
        model.add(book, model.getProperty(NS_BOOK + "hasReadingLevel"),
                model.getResource(NS_BOOK + readingLevel));

        for (String genre : genres) {
            model.add(book, model.getProperty(NS_BOOK + "hasGenre"),
                    model.getResource(NS_BOOK + genre));
        }

        saveModel(model);
    }
}