package com.semweb.semanticwebhw2.controller;

import com.semweb.semanticwebhw2.service.RdfService;
import org.apache.jena.rdf.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Controller
public class RdfController {

    @Autowired
    private RdfService rdfService;

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
                triple.put("subject", shorten(stmt.getSubject().toString()));
                triple.put("predicate", shorten(stmt.getPredicate().toString()));
                triple.put("object", shorten(stmt.getObject().toString()));
                triples.add(triple);
            }

            springModel.addAttribute("triples", triples);
            return "upload-rdf";

        } catch (Exception e) {
            springModel.addAttribute("error", "Failed to parse RDF file: " + e.getMessage());
            return "upload-rdf";
        }
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        model.addAttribute("books", rdfService.getAllBooks());
        return "books";
    }

    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable String id, Model model) {
        Map<String, String> book = rdfService.getBookById(id);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        return "book-detail";
    }

    @GetMapping("/books/add")
    public String addBookPage() {
        return "book-form";
    }

    @PostMapping("/books/add")
    public String addBook(
            @RequestParam String id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String readingLevel,
            @RequestParam(required = false) List<String> genres,
            Model model) {
        try {
            rdfService.addBook(id, title, author, readingLevel,
                    genres != null ? genres : new ArrayList<>());
            return "redirect:/books";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "book-form";
        }
    }

    @GetMapping("/books/edit/{id}")
    public String editBookPage(@PathVariable String id, Model model) {
        Map<String, String> book = rdfService.getBookById(id);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        return "book-form";
    }

    @PostMapping("/books/edit/{id}")
    public String editBook(
            @PathVariable String id,
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String readingLevel,
            @RequestParam(required = false) List<String> genres,
            Model model) {
        try {
            rdfService.updateBook(id, title, author, readingLevel,
                    genres != null ? genres : new ArrayList<>());
            return "redirect:/books";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "book-form";
        }
    }

    private String shorten(String uri) {
        if (uri.contains("#")) return uri.substring(uri.lastIndexOf("#") + 1);
        return uri;
    }
}