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
    declarative: "#!declarative\nconst Anakin, Luke, Leia\nvar x, y, z\nrel/2 infix fatherOf\nAnakin fatherOf Luke\nAnakin fatherOf Leia\nsibling(x, y) = fatherOf(z, x) & fatherOf(z, y) & not x = y\nask x sibling(Luke, x)",
    prolog: "#!prolog\nfather_of(anakin, luke).\nfather_of(anakin, leia).\n?- father_of(anakin, Y).",
    flix: "#!flix\nparent(alice, bob).\nparent(bob, carol).\nancestor(X, Y) :- parent(X, Y).\nquery ancestor(alice, Y).",
    cypher: "#!cypher\n// Scenario: two distinct cities.\n// neq edge means the nodes are constrained to differ.\n// WHERE grounds both; RETURN a yields the source city.\n// Expected result: Paris\nMATCH (a)-[:neq]->(b) WHERE a = \"Paris\" AND b = \"Berlin\" RETURN a",
    kinship: "#!prolog\nparent(pam, bob).\nparent(tom, bob).\nparent(bob, ann).\n\ngrandparent(G, C) :- parent(G, P), parent(P, C).\n?- grandparent(pam, X).",
    reachability: "#!cypher\n// Chain: A --neq--> B --neq--> C (2-hop path).\n// neq edges constrain adjacent nodes to differ.\n// WHERE pins all three; RETURN c gives the end of the chain.\n// Expected result: C\nMATCH (a)-[:neq]->(b), (b)-[:neq]->(c) WHERE a = \"A\" AND b = \"B\" AND c = \"C\" RETURN c",
    list_ops: "#!declarative\nvar h, t\nask h [h | t] = [1, 2, 3]",
    append_o: "#!prolog\n?- append_o([1, 2], [3], X).",
    zebra_mini: "#!declarative\nconst Red, Blue\nvar x, y, z\nrel/2 infix leftOf\nRed leftOf Blue\nBlue leftOf Red\nneq(x, y) = not x = y\nask x leftOf(Red, x) & neq(x, Red)",
    cypher_filter: "#!cypher\nMATCH (a)-[:neq]->(b)\nWHERE a = \"London\" AND b <> 30\nRETURN a"
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
