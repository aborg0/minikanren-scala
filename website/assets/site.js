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
    if (elements.select) {
      elements.select.addEventListener("change", function () {
        elements.source.value = dslTemplates[elements.select.value] || "";
      });
    }

    elements.runButton.addEventListener("click", function () {
      var response = MiniKanrenWebsite.runLanguage(elements.source.value);
      setResult(elements, response);
    });
  }

  var scala3Templates = {
    equality: "5",
    disjunction: "1, 2, 3",
    contains: "value: 2\nlist: 1, 2, 3",
    append: "left: 1, 2\nright: 3, 4",
    reify_recursive: "5"
  };

  var dslTemplates = {
    legacy: "run -1 x {\n  member_o(x, [1, 2, 3])\n}",
    declarative: "#!declarative\nrun -1 x where {\n  x = 1 or x = 2\n}",
    prolog: "#!prolog\nrun -1 x { member_o(x, [1, 2, 3]). }",
    flix: "#!flix\nrun -1 p {\n  Parent(\"alice\", \"bob\").\n  Parent(\"bob\", \"charlie\").\n  Ancestor(x, y) :- Parent(x, y).\n  Ancestor(x, z) :- Parent(x, y), Ancestor(y, z).\n  Ancestor(\"alice\", p)?\n}",
    cypher: "#!cypher\nMATCH (a {name: \"alice\"})-[:parent]->(b)\nRETURN b",
    kinship: "#!prolog\n% Kinship rules\nparent(pam, bob).\nparent(tom, bob).\nparent(bob, ann).\n\ngrandparent(G, C) :- parent(G, P), parent(P, C).\n\n% Find all grandchildren of pam\nrun -1 x { grandparent(pam, x). }",
    reachability: "#!cypher\n// Find paths up to 3 hops away\nMATCH (a {id: 1})-[:edge]->(b)-[:edge]->(c)-[:edge]->(d)\nRETURN d",
    list_ops: "#!declarative\nrun -1 q where {\n  q = [1, 2] ++ [3, 4]\n}",
    append_o: "#!prolog\n% Standard logic programming append\nappend([], L, L).\nappend([H|T], L, [H|R]) :- append(T, L, R).\n\n% Run in reverse: find all pairs that sum to [1, 2, 3]\nrun -1 [x, y] { append(x, y, [1, 2, 3]). }",
    zebra_mini: "#!declarative\n% A tiny fragment of a zebra puzzle\nrun -1 [h1, h2] where {\n  h1 = \"red\" and h2 = \"blue\"\n  or\n  h1 = \"blue\" and h2 = \"red\"\n}",
    cypher_filter: "#!cypher\nMATCH (n:Person)\nWHERE n.age > 30 AND n.city = \"London\"\nRETURN n.name"
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
