"""Offline tests for the semantic search tool: chunking, incremental builds, ranking and the MCP protocol. No API calls."""

import hashlib
import io
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import numpy as np  # noqa: E402
import semsearch as s  # noqa: E402
import server  # noqa: E402

JAVA = '''package a.b;

import java.util.List;

/** A gun. */
public final class Demo {
    private static final String BRACES = "}{;";
    private final char c = '}';
    private int[] values = {1, 2, 3};

    // a comment with } and ;
    public void run() {
        List<Runnable> r = null;
        r.forEach(x -> { x.run(); });
    }

    /* block } comment */
    @Override
    public String toString() {
        return """
            text block } ;
            """;
    }

    public enum Mode { A, B }
}
'''


class FakeEmbedder:
    """Deterministic bag-of-words vectors, so related texts land close together."""

    def __init__(self):
        self.calls = 0
        self.embedded = 0

    @staticmethod
    def vec(text):
        v = np.zeros(64, np.float32)
        for w in text.lower().split():
            v[int(hashlib.md5(w.strip(".,:;()").encode()).hexdigest(), 16) % 64] += 1
        return v

    def documents(self, texts):
        self.calls += 1
        self.embedded += len(texts)
        return [self.vec(t) for t in texts]

    def query(self, text):
        return self.vec(text)


def git(root, *args):
    subprocess.run(["git", "-C", str(root)] + list(args), check=True, capture_output=True)


class Repo:
    def __enter__(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        git(self.root, "init", "-q")
        git(self.root, "config", "user.email", "t@example.com")
        git(self.root, "config", "user.name", "t")
        self.write("docs/recoil.md", "# Recoil\n\n## Kick\n\n" + "The rifle kicks up when fired fast.\n" * 12
                   + "\n## Ammo\n\nAmmo refills the rifle magazine.\n" + "more pad text\n" * 40)
        self.write("src/main/resources/assets/gregguns/lang/en_US.lang", "item.gregguns.coin.name=GregGuns Coin\n")
        self.write("src/Demo.java", JAVA)
        self.write("notes.txt", "not indexed")
        self.write("LICENSES/MIT.md", "license text")
        self.write("THIRD-PARTY-NOTICES.md", "notice text")
        git(self.root, "add", "-A")
        git(self.root, "commit", "-qm", "init")
        return self

    def write(self, rel, text):
        p = self.root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding="utf-8")

    def __exit__(self, *exc):
        self.tmp.cleanup()


class ChunkingTest(unittest.TestCase):
    def test_markdown_breadcrumbs_and_fences(self):
        text = "# Top\n\nintro\n\n## A\n\n```\n# not a heading\n```\n\n### A1\n\nx\n\n## B\n\ny\n"
        titles = [t for _, _, t in s.markdown_spans(text)]
        self.assertEqual(titles, ["Top", "Top > A", "Top > A > A1", "Top > B"])

    def test_java_members_ignore_braces_in_strings_and_comments(self):
        spans = s.java_spans(JAVA, "src/Demo.java")
        titles = [t for _, _, t in spans]
        for want in ("Demo: public void run()", "Demo: public String toString()", "Demo: public enum Mode { A, B }"):
            self.assertIn(want, titles)
        lines = JAVA.split("\n")
        run = next(sp for sp in spans if sp[2] == "Demo: public void run()")
        body = "\n".join(lines[run[0]:run[1]])
        self.assertIn("r.forEach", body)
        self.assertNotIn("toString", body)

    def test_chunks_cover_lines_once_and_respect_size(self):
        text = "# T\n\n" + "\n".join("line %d with some words" % i for i in range(2000))
        chunks = s.chunk_file("docs/big.md", text)
        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(c["text"]) <= s.MAX_CHARS for c in chunks))
        covered = [n for c in chunks for n in range(c["start"], c["end"] + 1)]
        self.assertEqual(len(covered), len(set(covered)))

    def test_small_sections_merge(self):
        chunks = s.chunk_file("docs/x.md", "# A\n\none\n\n## B\n\ntwo\n\n## C\n\nthree\n")
        self.assertEqual(len(chunks), 1)
        self.assertEqual(chunks[0]["title"], "A")

    def test_lang_file_is_its_own_kind(self):
        chunks = s.chunk_file("src/main/resources/assets/gregguns/lang/en_US.lang", "item.gregguns.coin.name=GregGuns Coin\n")
        self.assertEqual([c["kind"] for c in chunks], ["lang"])

    def test_python_spans(self):
        text = "import os\n\n\n@dec\ndef f():\n    pass\n\n\nclass K:\n    pass\n"
        self.assertEqual([t for _, _, t in s.python_spans(text)], ["", "def f():", "class K:"])


class IndexTest(unittest.TestCase):
    def test_corpus_is_tracked_supported_files_only(self):
        with Repo() as r:
            r.write("untracked.md", "secret notes")
            self.assertEqual(s.corpus_files(r.root), ["docs/recoil.md", "src/Demo.java", "src/main/resources/assets/gregguns/lang/en_US.lang"])

    def test_incremental_build_and_search(self):
        with Repo() as r:
            path = r.root / "reference" / "idx.npz"
            fake = FakeEmbedder()
            first = s.build(r.root, fake, path)
            self.assertEqual(fake.embedded, first["meta"]["chunks"])
            self.assertEqual(first["vectors"].shape[0], len(first["chunks"]))

            again = FakeEmbedder()
            s.build(r.root, again, path)
            self.assertEqual(again.embedded, 0)

            r.write("docs/recoil.md", (r.root / "docs/recoil.md").read_text() + "\n## Scope\n\nthe scope zooms the sight\n")
            third = FakeEmbedder()
            index = s.build(r.root, third, path)
            self.assertEqual(third.embedded, 1)

            hits = s.search(index, FakeEmbedder.vec("the rifle kicks up when fired fast"), k=3)
            self.assertEqual(hits[0]["path"], "docs/recoil.md")
            self.assertIn("kicks", hits[0]["text"])
            self.assertTrue(all(h["kind"] == "java" for h in s.search(index, FakeEmbedder.vec("run"), kind="java")))
            self.assertEqual(s.search(index, FakeEmbedder.vec("x"), path_prefix="nowhere/"), [])

    def test_per_file_cap(self):
        chunks = [{"path": "a", "kind": "docs", "text": str(i)} for i in range(5)] + [{"path": "b", "kind": "docs", "text": "b"}]
        index = {"chunks": chunks, "vectors": np.ones((6, 4), np.float32) / 2}
        hits = s.search(index, np.ones(4), k=10, per_file=2)
        self.assertEqual([h["path"] for h in hits], ["a", "a", "b"])

    def test_mixed_search_keeps_docs_slots_and_score_order(self):
        chunks = [{"path": "J%d.java" % i, "kind": "java", "text": ""} for i in range(9)] + \
                 [{"path": "d%d.md" % i, "kind": "docs", "text": ""} for i in range(3)]
        vectors = np.array([[1.0, 0.1 * i] for i in range(9)] + [[0.1, 1.0]] * 3, np.float32)
        index = {"chunks": chunks, "vectors": s.normalize(vectors)}
        hits = s.search(index, np.array([1.0, 0.0]), k=6)
        self.assertEqual(sum(h["kind"] == "docs" for h in hits), 2)
        self.assertEqual([h["score"] for h in hits], sorted((h["score"] for h in hits), reverse=True))
        self.assertTrue(all(h["kind"] == "java" for h in s.search(index, np.array([1.0, 0.0]), k=6, kind="java")))

    def test_type_name_ignores_class_in_comments(self):
        text = "/** The outside class rule. */\n@Tag(\"x\")\npublic class Real {\n  void f() {}\n}\n"
        self.assertTrue(s.java_spans(text, "Real.java")[-1][2].startswith("Real"))

    def test_load_key_from_env_file(self):
        with tempfile.TemporaryDirectory() as d:
            Path(d, ".env").write_text("# c\nOPENROUTER_API_KEY='abc'\n")
            old = os.environ.pop("OPENROUTER_API_KEY", None)
            try:
                self.assertEqual(s.load_key(d), "abc")
            finally:
                if old is not None:
                    os.environ["OPENROUTER_API_KEY"] = old


class ServerTest(unittest.TestCase):
    def test_protocol(self):
        with Repo() as r:
            path = r.root / "idx.npz"
            s.build(r.root, FakeEmbedder(), path)
            svc = server.Service(r.root, FakeEmbedder(), path)
            svc.last_refresh = 1e12
            init = server.handle(svc, {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-06-18"}})
            self.assertEqual(init["result"]["protocolVersion"], "2025-06-18")
            self.assertIsNone(server.handle(svc, {"jsonrpc": "2.0", "method": "notifications/initialized"}))
            names = [t["name"] for t in server.handle(svc, {"jsonrpc": "2.0", "id": 2, "method": "tools/list"})["result"]["tools"]]
            self.assertIn("semantic_search", names)
            res = server.handle(svc, {"jsonrpc": "2.0", "id": 3, "method": "tools/call",
                                      "params": {"name": "semantic_search", "arguments": {"query": "the rifle kicks up when fired fast", "k": 2}}})["result"]
            self.assertFalse(res["isError"])
            self.assertTrue(res["content"][0]["text"].startswith("[1] docs/recoil.md:"))
            status = server.handle(svc, {"jsonrpc": "2.0", "id": 4, "method": "tools/call",
                                         "params": {"name": "semantic_index_status", "arguments": {}}})["result"]
            self.assertIn("chunks", status["content"][0]["text"])
            self.assertIn("error", server.handle(svc, {"jsonrpc": "2.0", "id": 5, "method": "bogus"}))

    def test_no_key_reports_cleanly(self):
        svc = server.Service(Path(HERE), None, Path(HERE) / "missing.npz")
        text, is_error = svc.call("semantic_search", {"query": "x"})
        self.assertTrue(is_error)
        self.assertIn("OPENROUTER_API_KEY", text)


if __name__ == "__main__":
    unittest.main()
