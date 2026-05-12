(function () {
  function formatResponse(response) {
    var lines = Array.isArray(response.results) ? response.results : [];
    return lines.length ? lines.join("\n") : "[]";
  }

  function setResult(elements, response) {
    elements.status.textContent = response.message || (response.ok ? "Done." : "Failed.");
    elements.output.textContent = formatResponse(response);
  }

  function bindLanguagePlayground(elements) {
    elements.runButton.addEventListener("click", function () {
      var response = MiniKanrenWebsite.runLanguage(elements.source.value);
      setResult(elements, response);
    });
  }

  var scala3Templates = {
    equality: "5",
    disjunction: "1, 2, 3",
    contains: "value: 2\nlist: 1, 2, 3",
    append: "left: 1, 2\nright: 3, 4"
  };

  function bindScala3Playground(elements) {
    elements.select.addEventListener("change", function () {
      elements.payload.value = scala3Templates[elements.select.value] || "";
    });

    elements.runButton.addEventListener("click", function () {
      var response = MiniKanrenWebsite.runScala3Demo(elements.select.value, elements.payload.value);
      setResult(elements, response);
    });
  }

  function bindExamplePlayground(elements) {
    elements.runButton.addEventListener("click", function () {
      elements.status.textContent = "Running example...";
      var response = MiniKanrenWebsite.runExample(elements.select.value);
      setResult(elements, response);
    });
  }

  function renderDocsIndex(root) {
    var entries = MiniKanrenWebsite.docsIndex();
    root.innerHTML = "";
    entries.forEach(function (entry) {
      var article = document.createElement("article");
      article.className = "card accent-sand";
      article.innerHTML = "<h2>" + entry.title + "</h2><p>Rendered from the generated mdoc markdown included in the site artifact.</p><a href=\"../" + entry.href + "\">Open document</a>";
      root.appendChild(article);
    });
  }

  function renderMarkdownDocument(elements) {
    var params = new URLSearchParams(window.location.search);
    var doc = params.get("doc");
    if (!doc) {
      elements.rootElement.textContent = "Missing doc query parameter.";
      return;
    }

    elements.titleElement.textContent = doc.split("/").pop();
    elements.pathElement.textContent = doc;

    fetch(doc)
      .then(function (response) {
        if (!response.ok) {
          throw new Error("Unable to load document.");
        }
        return response.text();
      })
      .then(function (markdown) {
        elements.rootElement.innerHTML = window.marked.parse(markdown);
      })
      .catch(function (error) {
        elements.rootElement.textContent = error.message;
      });
  }

  window.SitePages = {
    bindLanguagePlayground: bindLanguagePlayground,
    bindScala3Playground: bindScala3Playground,
    bindExamplePlayground: bindExamplePlayground,
    renderDocsIndex: renderDocsIndex,
    renderMarkdownDocument: renderMarkdownDocument
  };
})();
