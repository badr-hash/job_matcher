// Shared Assistant UI logic (no framework)
(() => {
  function qs(id) {
    return document.getElementById(id);
  }

  function escapeHtml(s) {
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;");
  }

  function appendMessage(messagesEl, who, text) {
    const wrapper = document.createElement("div");
    wrapper.className = "assistant-msg";

    const badge = document.createElement("div");
    badge.className = "assistant-badge " + (who === "user" ? "user" : "bot");
    badge.textContent = who === "user" ? "U" : "AI";

    const bubble = document.createElement("div");
    bubble.className = "assistant-bubble";
    bubble.innerHTML = escapeHtml(text);

    wrapper.appendChild(badge);
    wrapper.appendChild(bubble);
    messagesEl.appendChild(wrapper);
    messagesEl.scrollTop = messagesEl.scrollHeight;
  }

  function setOpen(panelEl, backdropEl, isOpen) {
    panelEl.style.display = isOpen ? "block" : "none";
    backdropEl.style.display = isOpen ? "block" : "none";
    backdropEl.setAttribute("aria-hidden", isOpen ? "false" : "true");
  }

  async function sendToAssistant(message, model) {
    const res = await fetch("/api/assistant/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, model }),
    });
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      const err = data?.error || `Erreur (${res.status})`;
      throw new Error(err);
    }
    return data?.reply ?? "";
  }

  function mount() {
    const fab = qs("assistantFab");
    const panel = qs("assistantPanel");
    const backdrop = qs("assistantBackdrop");
    const close = qs("assistantClose");
    const input = qs("assistantInput");
    const send = qs("assistantSend");
    const messages = qs("assistantMessages");
    const modelSelect = qs("assistantModel");

    if (!fab || !panel || !backdrop || !close || !input || !send || !messages) return;

    const defaultModel = modelSelect?.value || "llama3";
    if (messages.childElementCount === 0) {
      appendMessage(messages, "bot", "Salut ! Je suis prêt. Si Ollama n'est pas démarré, je te dirai quoi faire.");
      appendMessage(messages, "bot", `Modèle: ${defaultModel}.`);
    }

    function open() {
      setOpen(panel, backdrop, true);
      setTimeout(() => input.focus(), 0);
    }
    function closePanel() {
      setOpen(panel, backdrop, false);
    }

    fab.addEventListener("click", open);
    close.addEventListener("click", closePanel);
    backdrop.addEventListener("click", closePanel);
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && panel.style.display === "block") closePanel();
    });

    async function doSend() {
      const msg = input.value.trim();
      if (!msg) return;

      const model = modelSelect?.value || "llama3";
      input.value = "";
      appendMessage(messages, "user", msg);

      send.disabled = true;
      input.disabled = true;
      try {
        const reply = await sendToAssistant(msg, model);
        appendMessage(messages, "bot", reply || "(réponse vide)");
      } catch (e) {
        appendMessage(messages, "bot", `Erreur: ${e?.message || "inconnue"}`);
        appendMessage(messages, "bot", "Vérifie que Ollama est lancé: `ollama serve` puis que le modèle est téléchargé.");
      } finally {
        send.disabled = false;
        input.disabled = false;
        input.focus();
      }
    }

    send.addEventListener("click", doSend);
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        doSend();
      }
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount);
  } else {
    mount();
  }
})();

