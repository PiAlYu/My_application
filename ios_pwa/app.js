(() => {
  "use strict";

  const STORAGE_KEY = "store_checklist_pwa_state_v1";
  const TOAST_DURATION_MS = 5200;

  const THEME_MODES = {
    SYSTEM: "SYSTEM",
    LIGHT: "LIGHT",
    DARK: "DARK",
  };

  const CHECKLIST_MODES = {
    HIDE_ON_TAP: "HIDE_ON_TAP",
    MARKER: "MARKER",
  };

  const ui = {
    route: { name: "role" },
    isSettingsOpen: false,
    adminSyncInProgress: false,
    userSyncInProgress: false,
    adminSelectedItemIds: new Set(),
    toastTimer: null,
    settingsMessage: "",
    completionByChecklistId: new Map(),
  };

  const elements = {};
  const state = loadState();

  init();

  function init() {
    cacheElements();
    bindEvents();
    applyTheme();
    ui.route = parseRoute();
    renderCurrentRoute();
    registerServiceWorker();
  }

  function cacheElements() {
    elements.screens = {
      role: document.getElementById("screen-role"),
      adminLists: document.getElementById("screen-admin-lists"),
      adminChecklist: document.getElementById("screen-admin-checklist"),
      userLists: document.getElementById("screen-user-lists"),
      userChecklist: document.getElementById("screen-user-checklist"),
    };

    elements.toast = document.getElementById("toast");

    elements.roleOpenSettingsBtn = document.getElementById("roleOpenSettingsBtn");
    elements.roleOpenSuperUserBtn = document.getElementById("roleOpenSuperUserBtn");
    elements.roleOpenAdminBtn = document.getElementById("roleOpenAdminBtn");
    elements.roleOpenUserBtn = document.getElementById("roleOpenUserBtn");

    elements.settingsModal = document.getElementById("settingsModal");
    elements.settingsCloseBtn = document.getElementById("settingsCloseBtn");
    elements.settingsThemeSystem = document.getElementById("settingsThemeSystem");
    elements.settingsThemeLight = document.getElementById("settingsThemeLight");
    elements.settingsThemeDark = document.getElementById("settingsThemeDark");
    elements.settingsModeHide = document.getElementById("settingsModeHide");
    elements.settingsModeMarker = document.getElementById("settingsModeMarker");
    elements.settingsServerUrl = document.getElementById("settingsServerUrl");
    elements.settingsReadToken = document.getElementById("settingsReadToken");
    elements.settingsWriteToken = document.getElementById("settingsWriteToken");
    elements.settingsSaveConnectionBtn = document.getElementById("settingsSaveConnectionBtn");
    elements.settingsConnectionMessage = document.getElementById("settingsConnectionMessage");

    elements.adminListsBackBtn = document.getElementById("adminListsBackBtn");
    elements.adminListsTitle = document.getElementById("adminListsTitle");
    elements.adminSyncBtn = document.getElementById("adminSyncBtn");
    elements.adminModeBanner = document.getElementById("adminModeBanner");
    elements.adminModeBannerTitle = document.getElementById("adminModeBannerTitle");
    elements.adminModeBannerBody = document.getElementById("adminModeBannerBody");
    elements.adminCreateForm = document.getElementById("adminCreateForm");
    elements.adminCreateTitle = document.getElementById("adminCreateTitle");
    elements.adminListsEmpty = document.getElementById("adminListsEmpty");
    elements.adminListsContainer = document.getElementById("adminListsContainer");

    elements.adminChecklistBackBtn = document.getElementById("adminChecklistBackBtn");
    elements.adminChecklistMissing = document.getElementById("adminChecklistMissing");
    elements.adminChecklistContent = document.getElementById("adminChecklistContent");
    elements.adminChecklistTitleInput = document.getElementById("adminChecklistTitleInput");
    elements.adminChecklistSaveTitleBtn = document.getElementById("adminChecklistSaveTitleBtn");
    elements.adminChecklistSingleInput = document.getElementById("adminChecklistSingleInput");
    elements.adminChecklistAddSingleBtn = document.getElementById("adminChecklistAddSingleBtn");
    elements.adminChecklistBatchInput = document.getElementById("adminChecklistBatchInput");
    elements.adminChecklistAddBatchBtn = document.getElementById("adminChecklistAddBatchBtn");
    elements.adminChecklistItemsEmpty = document.getElementById("adminChecklistItemsEmpty");
    elements.adminChecklistItemsContainer = document.getElementById("adminChecklistItemsContainer");
    elements.adminChecklistDeleteSelectedBtn = document.getElementById("adminChecklistDeleteSelectedBtn");

    elements.userListsBackBtn = document.getElementById("userListsBackBtn");
    elements.userSyncBtn = document.getElementById("userSyncBtn");
    elements.userListsEmpty = document.getElementById("userListsEmpty");
    elements.userListsContainer = document.getElementById("userListsContainer");

    elements.userChecklistBackBtn = document.getElementById("userChecklistBackBtn");
    elements.userChecklistTitle = document.getElementById("userChecklistTitle");
    elements.userChecklistMissing = document.getElementById("userChecklistMissing");
    elements.userChecklistContent = document.getElementById("userChecklistContent");
    elements.userChecklistResetBtn = document.getElementById("userChecklistResetBtn");
    elements.userChecklistCompletionCard = document.getElementById("userChecklistCompletionCard");
    elements.userChecklistCompletionText = document.getElementById("userChecklistCompletionText");
    elements.userChecklistHelperText = document.getElementById("userChecklistHelperText");
    elements.userChecklistBadge = document.getElementById("userChecklistBadge");
    elements.userChecklistItemsEmpty = document.getElementById("userChecklistItemsEmpty");
    elements.userChecklistItemsContainer = document.getElementById("userChecklistItemsContainer");
    elements.userCelebrationOverlay = document.getElementById("userCelebrationOverlay");
  }

  function bindEvents() {
    window.addEventListener("hashchange", () => {
      if (ui.isSettingsOpen) {
        closeSettings();
      }
      ui.route = parseRoute();
      if (ui.route.name !== "admin-checklist") {
        ui.adminSelectedItemIds = new Set();
      }
      renderCurrentRoute();
    });

    elements.roleOpenSettingsBtn.addEventListener("click", openSettings);
    elements.roleOpenSuperUserBtn.addEventListener("click", () => navigate({ name: "superuser-lists" }));
    elements.roleOpenAdminBtn.addEventListener("click", () => navigate({ name: "admin-lists" }));
    elements.roleOpenUserBtn.addEventListener("click", () => navigate({ name: "user-lists" }));

    elements.settingsCloseBtn.addEventListener("click", closeSettings);
    elements.settingsModal.addEventListener("click", (event) => {
      if (event.target === elements.settingsModal) {
        closeSettings();
      }
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && ui.isSettingsOpen) {
        closeSettings();
      }
    });

    [elements.settingsThemeSystem, elements.settingsThemeLight, elements.settingsThemeDark].forEach((node) => {
      node.addEventListener("change", onThemeChanged);
    });
    [elements.settingsModeHide, elements.settingsModeMarker].forEach((node) => {
      node.addEventListener("change", onChecklistModeChanged);
    });
    elements.settingsSaveConnectionBtn.addEventListener("click", saveServerConnectionFromDraft);

    elements.adminListsBackBtn.addEventListener("click", () => navigate({ name: "role" }));
    elements.adminSyncBtn.addEventListener("click", onAdminSyncClicked);
    elements.adminCreateForm.addEventListener("submit", onAdminCreateSubmit);
    elements.adminListsContainer.addEventListener("click", onAdminListsContainerClick);

    elements.adminChecklistBackBtn.addEventListener("click", () => navigateToAdminListsFromChecklist());
    elements.adminChecklistSaveTitleBtn.addEventListener("click", onAdminSaveTitleClicked);
    elements.adminChecklistAddSingleBtn.addEventListener("click", onAdminAddSingleClicked);
    elements.adminChecklistAddBatchBtn.addEventListener("click", onAdminAddBatchClicked);
    elements.adminChecklistItemsContainer.addEventListener("click", onAdminChecklistItemsClick);
    elements.adminChecklistItemsContainer.addEventListener("change", onAdminChecklistItemCheckboxChange);
    elements.adminChecklistDeleteSelectedBtn.addEventListener("click", onAdminDeleteSelectedClicked);

    elements.userListsBackBtn.addEventListener("click", () => navigate({ name: "role" }));
    elements.userSyncBtn.addEventListener("click", onUserSyncClicked);
    elements.userListsContainer.addEventListener("click", onUserListsContainerClick);

    elements.userChecklistBackBtn.addEventListener("click", () => navigate({ name: "user-lists" }));
    elements.userChecklistResetBtn.addEventListener("click", onUserResetProgressClicked);
    elements.userChecklistItemsContainer.addEventListener("click", onUserItemRowClicked);

    if (window.matchMedia) {
      window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => {
        if (state.settings.themeMode === THEME_MODES.SYSTEM) {
          applyTheme();
        }
      });
    }
  }

  function parseRoute() {
    const hash = window.location.hash.startsWith("#")
      ? window.location.hash.slice(1)
      : window.location.hash;
    const normalized = hash.startsWith("/") ? hash.slice(1) : hash;
    const parts = normalized.split("/").filter(Boolean);

    if (parts.length === 0 || parts[0] === "role") {
      return { name: "role" };
    }
    if (parts[0] === "admin-lists") {
      return { name: "admin-lists" };
    }
    if (parts[0] === "superuser-lists") {
      return { name: "superuser-lists" };
    }
    if (parts[0] === "admin-checklist" && parts[1]) {
      const checklistId = Number.parseInt(parts[1], 10);
      if (Number.isFinite(checklistId)) {
        return { name: "admin-checklist", checklistId };
      }
    }
    if (parts[0] === "user-lists") {
      return { name: "user-lists" };
    }
    if (parts[0] === "user-checklist" && parts[1]) {
      const checklistId = Number.parseInt(parts[1], 10);
      if (Number.isFinite(checklistId)) {
        return { name: "user-checklist", checklistId };
      }
    }
    return { name: "role" };
  }

  function routeToHash(route) {
    switch (route.name) {
      case "role":
        return "#/role";
      case "admin-lists":
        return "#/admin-lists";
      case "superuser-lists":
        return "#/superuser-lists";
      case "admin-checklist":
        return "#/admin-checklist/" + route.checklistId;
      case "user-lists":
        return "#/user-lists";
      case "user-checklist":
        return "#/user-checklist/" + route.checklistId;
      default:
        return "#/role";
    }
  }

  function navigate(route) {
    const nextHash = routeToHash(route);
    if (window.location.hash !== nextHash) {
      window.location.hash = nextHash;
      return;
    }
    ui.route = parseRoute();
    renderCurrentRoute();
  }

  function navigateToAdminListsFromChecklist() {
    const target = wasOpenedFromSuperUser()
      ? { name: "superuser-lists" }
      : { name: "admin-lists" };
    navigate(target);
  }

  function wasOpenedFromSuperUser() {
    return window.sessionStorage.getItem("store_checklist_last_admin_mode") === "superuser";
  }

  function markAdminModeInSession(isSuperUserMode) {
    window.sessionStorage.setItem(
      "store_checklist_last_admin_mode",
      isSuperUserMode ? "superuser" : "admin",
    );
  }

  function renderCurrentRoute() {
    hideAllScreens();

    switch (ui.route.name) {
      case "role":
        elements.screens.role.classList.remove("hidden");
        renderRoleScreen();
        break;
      case "admin-lists":
        elements.screens.adminLists.classList.remove("hidden");
        renderAdminListsScreen(false);
        break;
      case "superuser-lists":
        elements.screens.adminLists.classList.remove("hidden");
        renderAdminListsScreen(true);
        break;
      case "admin-checklist":
        elements.screens.adminChecklist.classList.remove("hidden");
        renderAdminChecklistScreen(ui.route.checklistId);
        break;
      case "user-lists":
        elements.screens.userLists.classList.remove("hidden");
        renderUserListsScreen();
        break;
      case "user-checklist":
        elements.screens.userChecklist.classList.remove("hidden");
        renderUserChecklistScreen(ui.route.checklistId);
        break;
      default:
        navigate({ name: "role" });
        break;
    }
  }

  function hideAllScreens() {
    Object.values(elements.screens).forEach((screen) => {
      screen.classList.add("hidden");
    });
  }

  function renderRoleScreen() {
    syncSettingsDraftToUi();
    if (ui.isSettingsOpen) {
      openSettings();
    }
  }

  function openSettings() {
    ui.isSettingsOpen = true;
    syncSettingsDraftToUi();
    elements.settingsModal.classList.remove("hidden");
  }

  function closeSettings() {
    ui.isSettingsOpen = false;
    elements.settingsModal.classList.add("hidden");
  }

  function syncSettingsDraftToUi() {
    const settings = state.settings;
    elements.settingsThemeSystem.checked = settings.themeMode === THEME_MODES.SYSTEM;
    elements.settingsThemeLight.checked = settings.themeMode === THEME_MODES.LIGHT;
    elements.settingsThemeDark.checked = settings.themeMode === THEME_MODES.DARK;

    elements.settingsModeHide.checked = settings.checklistMode === CHECKLIST_MODES.HIDE_ON_TAP;
    elements.settingsModeMarker.checked = settings.checklistMode === CHECKLIST_MODES.MARKER;

    elements.settingsServerUrl.value = settings.serverBaseUrl;
    elements.settingsReadToken.value = settings.readToken;
    elements.settingsWriteToken.value = settings.writeToken;
    elements.settingsConnectionMessage.textContent = ui.settingsMessage;
  }

  function onThemeChanged(event) {
    const nextTheme = normalizeThemeMode(event.target.value);
    state.settings.themeMode = nextTheme;
    saveState();
    applyTheme();
  }

  function onChecklistModeChanged(event) {
    const nextMode = normalizeChecklistMode(event.target.value);
    state.settings.checklistMode = nextMode;
    saveState();
  }

  function saveServerConnectionFromDraft() {
    const rawUrl = elements.settingsServerUrl.value;
    const rawReadToken = elements.settingsReadToken.value;
    const rawWriteToken = elements.settingsWriteToken.value;

    try {
      const normalizedUrl = normalizeBaseUrl(rawUrl);
      state.settings.serverBaseUrl = normalizedUrl;
      state.settings.readToken = normalizeToken(rawReadToken);
      state.settings.writeToken = normalizeToken(rawWriteToken);
      saveState();

      ui.settingsMessage = "Подключение сохранено.";
      elements.settingsConnectionMessage.textContent = ui.settingsMessage;
      showToast("Настройки сервера сохранены.");
    } catch (error) {
      ui.settingsMessage = "Подключение не сохранено. " + error.message;
      elements.settingsConnectionMessage.textContent = ui.settingsMessage;
    }
  }

  function renderAdminListsScreen(isSuperUserMode) {
    markAdminModeInSession(isSuperUserMode);

    elements.adminListsTitle.textContent = isSuperUserMode
      ? "Super user: управление"
      : "Управление списками";

    elements.adminModeBanner.classList.toggle("superuser", isSuperUserMode);
    if (isSuperUserMode) {
      elements.adminModeBannerTitle.textContent = "Режим super user";
      elements.adminModeBannerBody.textContent =
        "Синхронизация полностью заменит базу сервера текущими локальными списками. " +
        "Если локальная база пуста, сервер тоже станет пустым.";
    } else {
      elements.adminModeBannerTitle.textContent = "Обычный режим подключения";
      elements.adminModeBannerBody.textContent =
        "Синхронизация добавляет на устройство только отсутствующие списки с сервера. " +
        "Локальные удаления и изменения на сервер не отправляются.";
    }

    elements.adminSyncBtn.disabled = ui.adminSyncInProgress;
    elements.adminSyncBtn.textContent = ui.adminSyncInProgress ? "..." : "Sync";

    const checklists = getChecklistsSortedByUpdatedAt();
    elements.adminListsContainer.innerHTML = "";
    elements.adminListsEmpty.classList.toggle("hidden", checklists.length > 0);

    checklists.forEach((checklist) => {
      const card = document.createElement("article");
      card.className = "card";
      card.innerHTML =
        "<div>" +
        "<h3>" + escapeHtml(checklist.title) + "</h3>" +
        "<p>Товаров: " + checklist.items.length + "</p>" +
        "<span class=\"tag\">" + (checklist.isFromServer ? "Есть на сервере" : "Только локально") + "</span>" +
        "</div>" +
        "<div class=\"card-actions\">" +
        "<button class=\"mini-btn\" type=\"button\" data-open-checklist-id=\"" + checklist.id + "\">Открыть</button>" +
        "<button class=\"mini-btn danger\" type=\"button\" data-delete-checklist-id=\"" + checklist.id + "\">Удалить</button>" +
        "</div>";
      elements.adminListsContainer.appendChild(card);
    });
  }

  function onAdminCreateSubmit(event) {
    event.preventDefault();
    const title = elements.adminCreateTitle.value;
    createChecklist(title);
    elements.adminCreateTitle.value = "";
    renderCurrentRoute();
  }

  function onAdminListsContainerClick(event) {
    const openBtn = event.target.closest("[data-open-checklist-id]");
    if (openBtn) {
      const checklistId = Number.parseInt(openBtn.getAttribute("data-open-checklist-id"), 10);
      if (Number.isFinite(checklistId)) {
        ui.adminSelectedItemIds = new Set();
        navigate({ name: "admin-checklist", checklistId });
      }
      return;
    }

    const deleteBtn = event.target.closest("[data-delete-checklist-id]");
    if (deleteBtn) {
      const checklistId = Number.parseInt(deleteBtn.getAttribute("data-delete-checklist-id"), 10);
      if (Number.isFinite(checklistId)) {
        deleteChecklist(checklistId);
        renderCurrentRoute();
      }
    }
  }

  async function onAdminSyncClicked() {
    if (ui.adminSyncInProgress) return;
    ui.adminSyncInProgress = true;
    renderCurrentRoute();

    const isSuperUserMode = ui.route.name === "superuser-lists";
    try {
      if (isSuperUserMode) {
        const report = await replaceServerWithLocal();
        showToast("Серверная база заменена локальной. Отправлено списков: " + report.addedToServer + ".");
      } else {
        const report = await importMissingFromServer();
        showToast("С сервера добавлено списков: " + report.addedToLocal + ".");
      }
    } catch (error) {
      if (isSuperUserMode) {
        showToast("Не удалось перезаписать сервер локальной базой. " + error.message);
      } else {
        showToast("Не удалось обновить списки с сервера. " + error.message);
      }
    } finally {
      ui.adminSyncInProgress = false;
      renderCurrentRoute();
    }
  }

  function renderAdminChecklistScreen(checklistId) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) {
      elements.adminChecklistMissing.classList.remove("hidden");
      elements.adminChecklistContent.classList.add("hidden");
      return;
    }

    elements.adminChecklistMissing.classList.add("hidden");
    elements.adminChecklistContent.classList.remove("hidden");

    if (elements.adminChecklistTitleInput.value === "" || elements.adminChecklistTitleInput.dataset.boundChecklistId !== String(checklistId)) {
      elements.adminChecklistTitleInput.value = checklist.title;
      elements.adminChecklistTitleInput.dataset.boundChecklistId = String(checklistId);
      elements.adminChecklistSingleInput.value = "";
      elements.adminChecklistBatchInput.value = "";
      ui.adminSelectedItemIds = new Set();
    }

    const sortedItems = [...checklist.items].sort(compareItemsByPosition);
    const selectedOnlyExisting = new Set();
    sortedItems.forEach((item) => {
      if (ui.adminSelectedItemIds.has(item.id)) {
        selectedOnlyExisting.add(item.id);
      }
    });
    ui.adminSelectedItemIds = selectedOnlyExisting;

    elements.adminChecklistItemsContainer.innerHTML = "";
    elements.adminChecklistItemsEmpty.classList.toggle("hidden", sortedItems.length > 0);

    sortedItems.forEach((item) => {
      const row = document.createElement("div");
      row.className = "row";
      row.innerHTML =
        "<label>" +
        "<input type=\"checkbox\" data-select-item-id=\"" + item.id + "\"" +
        (ui.adminSelectedItemIds.has(item.id) ? " checked" : "") +
        ">" +
        "<span class=\"item-name\">" + escapeHtml(item.name) + "</span>" +
        "</label>" +
        "<button class=\"mini-btn danger\" type=\"button\" data-delete-item-id=\"" + item.id + "\">Удалить</button>";
      elements.adminChecklistItemsContainer.appendChild(row);
    });

    elements.adminChecklistDeleteSelectedBtn.disabled = ui.adminSelectedItemIds.size === 0;
    elements.adminChecklistDeleteSelectedBtn.textContent =
      "Удалить выбранные (" + ui.adminSelectedItemIds.size + ")";
  }

  function onAdminSaveTitleClicked() {
    if (ui.route.name !== "admin-checklist") return;
    renameChecklist(ui.route.checklistId, elements.adminChecklistTitleInput.value);
    renderCurrentRoute();
  }

  function onAdminAddSingleClicked() {
    if (ui.route.name !== "admin-checklist") return;
    addItems(ui.route.checklistId, [elements.adminChecklistSingleInput.value]);
    elements.adminChecklistSingleInput.value = "";
    renderCurrentRoute();
  }

  function onAdminAddBatchClicked() {
    if (ui.route.name !== "admin-checklist") return;
    const names = elements.adminChecklistBatchInput.value.split("\n");
    addItems(ui.route.checklistId, names);
    elements.adminChecklistBatchInput.value = "";
    renderCurrentRoute();
  }

  function onAdminChecklistItemsClick(event) {
    const deleteBtn = event.target.closest("[data-delete-item-id]");
    if (!deleteBtn) return;
    if (ui.route.name !== "admin-checklist") return;

    const itemId = Number.parseInt(deleteBtn.getAttribute("data-delete-item-id"), 10);
    if (!Number.isFinite(itemId)) return;
    deleteItems(ui.route.checklistId, [itemId]);
    ui.adminSelectedItemIds.delete(itemId);
    renderCurrentRoute();
  }

  function onAdminChecklistItemCheckboxChange(event) {
    const checkbox = event.target.closest("[data-select-item-id]");
    if (!checkbox) return;

    const itemId = Number.parseInt(checkbox.getAttribute("data-select-item-id"), 10);
    if (!Number.isFinite(itemId)) return;

    if (checkbox.checked) {
      ui.adminSelectedItemIds.add(itemId);
    } else {
      ui.adminSelectedItemIds.delete(itemId);
    }
    renderCurrentRoute();
  }

  function onAdminDeleteSelectedClicked() {
    if (ui.route.name !== "admin-checklist") return;
    const selectedIds = [...ui.adminSelectedItemIds];
    if (selectedIds.length === 0) return;
    deleteItems(ui.route.checklistId, selectedIds);
    ui.adminSelectedItemIds = new Set();
    renderCurrentRoute();
  }

  function renderUserListsScreen() {
    elements.userSyncBtn.disabled = ui.userSyncInProgress;
    elements.userSyncBtn.textContent = ui.userSyncInProgress ? "..." : "Sync";

    const checklists = getChecklistsSortedByUpdatedAt();
    elements.userListsContainer.innerHTML = "";
    elements.userListsEmpty.classList.toggle("hidden", checklists.length > 0);

    checklists.forEach((checklist) => {
      const card = document.createElement("article");
      card.className = "card";
      card.innerHTML =
        "<div>" +
        "<h3>" + escapeHtml(checklist.title) + "</h3>" +
        "<p>Товаров: " + checklist.items.length + "</p>" +
        "</div>" +
        "<div class=\"card-actions\">" +
        "<button class=\"mini-btn\" type=\"button\" data-open-user-checklist-id=\"" + checklist.id + "\">Открыть</button>" +
        "</div>";
      elements.userListsContainer.appendChild(card);
    });
  }

  function onUserListsContainerClick(event) {
    const openBtn = event.target.closest("[data-open-user-checklist-id]");
    if (!openBtn) return;

    const checklistId = Number.parseInt(openBtn.getAttribute("data-open-user-checklist-id"), 10);
    if (Number.isFinite(checklistId)) {
      navigate({ name: "user-checklist", checklistId });
    }
  }

  async function onUserSyncClicked() {
    if (ui.userSyncInProgress) return;
    ui.userSyncInProgress = true;
    renderCurrentRoute();

    try {
      const report = await importMissingFromServer();
      showToast("С сервера добавлено списков: " + report.addedToLocal + ".");
    } catch (error) {
      showToast("Не удалось обновить списки. " + error.message);
    } finally {
      ui.userSyncInProgress = false;
      renderCurrentRoute();
    }
  }

  function renderUserChecklistScreen(checklistId) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) {
      elements.userChecklistMissing.classList.remove("hidden");
      elements.userChecklistContent.classList.add("hidden");
      return;
    }

    elements.userChecklistMissing.classList.add("hidden");
    elements.userChecklistContent.classList.remove("hidden");

    const mode = state.settings.checklistMode;
    const orderedItems = [...checklist.items].sort(compareItemsByPosition);
    const completedItems = orderedItems.filter((item) => isItemCompleted(checklist, item.id, mode)).length;
    const totalItems = orderedItems.length;
    const isCompleted = totalItems > 0 && completedItems === totalItems;

    const previousCompletion = ui.completionByChecklistId.get(checklistId);
    ui.completionByChecklistId.set(checklistId, isCompleted);
    if (previousCompletion !== undefined && previousCompletion === false && isCompleted) {
      triggerCelebration();
    }

    const visibleItems = mode === CHECKLIST_MODES.HIDE_ON_TAP
      ? orderedItems.filter((item) => !isItemHiddenInHideMode(checklist, item.id))
      : orderedItems;

    elements.userChecklistTitle.textContent = checklist.title || "Список";
    elements.userChecklistCompletionCard.classList.toggle("hidden", !isCompleted);
    elements.userChecklistCompletionText.textContent = "Все " + totalItems + " товаров пройдены.";

    const helperText = isCompleted
      ? "Все товары отмечены. Можно сбросить прогресс и пройти заново."
      : mode === CHECKLIST_MODES.HIDE_ON_TAP
        ? "Нажмите на товар, и он исчезнет из текущего списка."
        : "Нажмите на товар, чтобы отметить или снять маркер.";
    elements.userChecklistHelperText.textContent = helperText;
    elements.userChecklistBadge.textContent = mode === CHECKLIST_MODES.HIDE_ON_TAP
      ? visibleItems.length + " осталось"
      : completedItems + "/" + totalItems;

    elements.userChecklistItemsContainer.innerHTML = "";
    elements.userChecklistItemsEmpty.classList.add("hidden");

    if (visibleItems.length === 0) {
      elements.userChecklistItemsEmpty.classList.remove("hidden");
      if (totalItems === 0) {
        elements.userChecklistItemsEmpty.textContent = "В списке пока нет товаров.";
      } else if (isCompleted) {
        elements.userChecklistItemsEmpty.textContent = "Список полностью пройден.";
      } else {
        elements.userChecklistItemsEmpty.textContent = "Все товары обработаны в текущем режиме.";
      }
      return;
    }

    visibleItems.forEach((item) => {
      const isMarked = mode === CHECKLIST_MODES.MARKER && isItemMarkedInMarkerMode(checklist, item.id);
      const row = document.createElement("button");
      row.type = "button";
      row.className = "row" + (isMarked ? " done" : "");
      row.setAttribute("data-user-item-id", String(item.id));
      row.innerHTML =
        "<span class=\"marker\">" + (mode === CHECKLIST_MODES.MARKER ? (isMarked ? "✓" : "○") : "•") + "</span>" +
        "<span class=\"item-name\">" + escapeHtml(item.name) + "</span>" +
        "<span class=\"tag\">" + (mode === CHECKLIST_MODES.HIDE_ON_TAP ? "Скрыть" : "Отметить") + "</span>";
      elements.userChecklistItemsContainer.appendChild(row);
    });
  }

  function onUserItemRowClicked(event) {
    const row = event.target.closest("[data-user-item-id]");
    if (!row) return;
    if (ui.route.name !== "user-checklist") return;

    const itemId = Number.parseInt(row.getAttribute("data-user-item-id"), 10);
    if (!Number.isFinite(itemId)) return;

    onUserTappedItem(ui.route.checklistId, itemId, state.settings.checklistMode);
    renderCurrentRoute();
  }

  function onUserResetProgressClicked() {
    if (ui.route.name !== "user-checklist") return;
    resetChecklistProgress(ui.route.checklistId);
    renderCurrentRoute();
  }

  function triggerCelebration() {
    elements.userCelebrationOverlay.classList.remove("hidden");
    elements.userCelebrationOverlay.classList.remove("celebration-overlay");
    void elements.userCelebrationOverlay.offsetWidth;
    elements.userCelebrationOverlay.classList.add("celebration-overlay");
    window.setTimeout(() => {
      elements.userCelebrationOverlay.classList.add("hidden");
    }, 2200);
  }

  function createChecklist(title) {
    const normalizedTitle = String(title || "").trim();
    if (normalizedTitle === "") return;

    state.checklists.push({
      id: nextChecklistId(),
      serverId: null,
      title: normalizedTitle,
      isFromServer: false,
      updatedAt: nowMs(),
      items: [],
      progress: {},
    });
    saveState();
  }

  function renameChecklist(checklistId, title) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) return;

    const normalizedTitle = String(title || "").trim();
    if (normalizedTitle === "") return;

    checklist.title = normalizedTitle;
    checklist.updatedAt = nowMs();
    saveState();
  }

  function deleteChecklist(checklistId) {
    const initialLength = state.checklists.length;
    state.checklists = state.checklists.filter((item) => item.id !== checklistId);
    if (state.checklists.length !== initialLength) {
      saveState();
    }
  }

  function addItems(checklistId, names) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) return;

    const normalized = normalizeItemNames(names);
    if (normalized.length === 0) return;

    const startPosition = checklist.items.length === 0
      ? 0
      : Math.max(...checklist.items.map((item) => item.position)) + 1;

    normalized.forEach((name, index) => {
      checklist.items.push({
        id: nextItemId(),
        name,
        position: startPosition + index,
      });
    });
    checklist.updatedAt = nowMs();
    saveState();
  }

  function deleteItems(checklistId, itemIds) {
    const checklist = getChecklistById(checklistId);
    if (!checklist || itemIds.length === 0) return;

    const idSet = new Set(itemIds);
    checklist.items = checklist.items.filter((item) => !idSet.has(item.id));

    Object.keys(checklist.progress).forEach((key) => {
      if (idSet.has(Number.parseInt(key, 10))) {
        delete checklist.progress[key];
      }
    });

    saveState();
  }

  function onUserTappedItem(checklistId, itemId, mode) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) return;

    const key = String(itemId);
    const current = checklist.progress[key] || {
      hiddenInHideMode: false,
      markedInMarkerMode: false,
    };

    if (mode === CHECKLIST_MODES.HIDE_ON_TAP) {
      checklist.progress[key] = {
        hiddenInHideMode: true,
        markedInMarkerMode: current.markedInMarkerMode,
      };
    } else {
      checklist.progress[key] = {
        hiddenInHideMode: current.hiddenInHideMode,
        markedInMarkerMode: !current.markedInMarkerMode,
      };
    }
    saveState();
  }

  function resetChecklistProgress(checklistId) {
    const checklist = getChecklistById(checklistId);
    if (!checklist) return;
    checklist.progress = {};
    saveState();
  }

  async function importMissingFromServer() {
    const remoteChecklists = await fetchServerChecklists(state.settings.serverBaseUrl, state.settings.readToken);
    const normalizedRemote = remoteChecklists.map(normalizeRemoteChecklist).filter(Boolean);

    const knownServerIds = new Set(
      state.checklists
        .map((checklist) => String(checklist.serverId || "").trim())
        .filter((serverId) => serverId !== ""),
    );
    const knownTitles = new Set(
      state.checklists
        .map((checklist) => normalizeTitleKey(checklist.title))
        .filter((title) => title !== ""),
    );

    let addedToLocal = 0;
    normalizedRemote.forEach((remoteChecklist) => {
      const titleKey = normalizeTitleKey(remoteChecklist.title);
      if (titleKey === "") return;
      if (knownServerIds.has(remoteChecklist.id) || knownTitles.has(titleKey)) return;

      const localChecklistId = nextChecklistId();
      const localChecklist = {
        id: localChecklistId,
        serverId: remoteChecklist.id,
        title: remoteChecklist.title,
        isFromServer: true,
        updatedAt: remoteChecklist.updatedAt || nowMs(),
        items: [],
        progress: {},
      };

      remoteChecklist.items.forEach((remoteItem, index) => {
        localChecklist.items.push({
          id: nextItemId(),
          name: remoteItem.name,
          position: index,
        });
      });

      state.checklists.push(localChecklist);
      knownServerIds.add(remoteChecklist.id);
      knownTitles.add(titleKey);
      addedToLocal += 1;
    });

    saveState();
    return {
      addedToLocal,
      addedToServer: 0,
      updatedOnServer: 0,
    };
  }

  async function replaceServerWithLocal() {
    const now = nowMs();
    const exportPlan = state.checklists
      .map((checklist) => buildExportChecklist(checklist, now))
      .filter(Boolean);

    const response = await replaceServerChecklists(
      state.settings.serverBaseUrl,
      resolveWriteToken(state.settings),
      exportPlan.map((item) => item.remoteChecklist),
    );

    const syncedRemoteById = new Map();
    response.map(normalizeRemoteChecklist).filter(Boolean).forEach((item) => {
      syncedRemoteById.set(item.id, item);
    });

    exportPlan.forEach((item) => {
      const localChecklist = getChecklistById(item.localChecklistId);
      if (!localChecklist) return;
      const syncedRemote = syncedRemoteById.get(item.remoteChecklist.id);

      localChecklist.serverId = item.remoteChecklist.id;
      localChecklist.isFromServer = true;
      localChecklist.updatedAt = syncedRemote && Number.isFinite(syncedRemote.updatedAt)
        ? syncedRemote.updatedAt
        : Math.max(localChecklist.updatedAt, now);
    });

    saveState();
    return {
      addedToLocal: 0,
      addedToServer: exportPlan.length,
      updatedOnServer: 0,
    };
  }

  function buildExportChecklist(localChecklist, now) {
    const normalizedTitle = String(localChecklist.title || "").trim();
    if (normalizedTitle === "") return null;

    const remoteChecklistId = String(localChecklist.serverId || "").trim() || generateUuid();
    const remoteChecklist = {
      id: remoteChecklistId,
      title: normalizedTitle,
      updatedAt: Math.max(now, Number(localChecklist.updatedAt) || now),
      items: normalizeItemNames(localChecklist.items.map((item) => item.name)).map((name) => ({ name })),
    };

    return {
      localChecklistId: localChecklist.id,
      remoteChecklist,
    };
  }

  async function fetchServerChecklists(baseUrl, token) {
    const endpoint = new URL("checklists", baseUrl).toString();
    const payload = await requestJson(endpoint, {
      method: "GET",
      token,
    });

    if (!Array.isArray(payload)) {
      throw new Error("Сервер вернул неожиданный формат ответа.");
    }
    return payload;
  }

  async function replaceServerChecklists(baseUrl, token, checklists) {
    const endpoint = new URL("checklists", baseUrl).toString();
    const payload = await requestJson(endpoint, {
      method: "PUT",
      token,
      body: checklists,
    });

    if (!Array.isArray(payload)) {
      throw new Error("Сервер вернул неожиданный формат ответа.");
    }
    return payload;
  }

  async function requestJson(url, options) {
    const method = options.method || "GET";
    const headers = {
      Accept: "application/json",
    };

    const normalizedToken = normalizeToken(options.token || "");
    if (normalizedToken !== "") {
      headers.Authorization = "Bearer " + normalizedToken;
    }
    if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    let response;
    try {
      response = await fetch(url, {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
      });
    } catch (error) {
      throw new Error("Сеть недоступна или сервер не отвечает.");
    }

    const text = await response.text();
    const body = safeParseJson(text);

    if (!response.ok) {
      const serverError = body && typeof body.error === "string" ? body.error : "";
      if (serverError) {
        throw new Error("HTTP " + response.status + ": " + serverError);
      }
      throw new Error("HTTP " + response.status + ".");
    }

    return body;
  }

  function normalizeRemoteChecklist(remoteChecklist) {
    if (!remoteChecklist || typeof remoteChecklist !== "object") return null;

    const id = String(remoteChecklist.id || "").trim();
    const title = String(remoteChecklist.title || "").trim();
    if (id === "") return null;

    const updatedAtRaw = Number(remoteChecklist.updatedAt);
    const updatedAt = Number.isFinite(updatedAtRaw) ? updatedAtRaw : nowMs();
    const remoteItems = Array.isArray(remoteChecklist.items) ? remoteChecklist.items : [];
    const items = normalizeItemNames(
      remoteItems.map((item) => {
        if (!item || typeof item !== "object") return "";
        return String(item.name || "");
      }),
    ).map((name) => ({ name }));

    return {
      id,
      title,
      updatedAt,
      items,
    };
  }

  function normalizeItemNames(rawItems) {
    return rawItems
      .map((item) => String(item || "").trim())
      .filter((item) => item !== "");
  }

  function normalizeTitleKey(rawTitle) {
    return String(rawTitle || "").trim();
  }

  function resolveWriteToken(settings) {
    const writeToken = normalizeToken(settings.writeToken || "");
    if (writeToken !== "") return writeToken;
    return normalizeToken(settings.readToken || "");
  }

  function normalizeBaseUrl(rawUrl) {
    const trimmed = String(rawUrl || "").trim();
    if (trimmed === "") {
      throw new Error("URL сервера не может быть пустым.");
    }

    const withSlash = trimmed.endsWith("/") ? trimmed : trimmed + "/";

    let parsedUrl;
    try {
      parsedUrl = new URL(withSlash);
    } catch (error) {
      throw new Error("Неверный формат URL.");
    }

    const scheme = String(parsedUrl.protocol || "").replace(":", "").toLowerCase();
    if (scheme !== "http" && scheme !== "https") {
      throw new Error("URL должен начинаться с http:// или https://");
    }

    const host = String(parsedUrl.hostname || "").trim();
    if (host === "") {
      throw new Error("В URL отсутствует host.");
    }

    if (scheme === "http" && requiresHttps(host)) {
      throw new Error("Для внешнего адреса используйте https://. HTTP оставлен только для localhost и локальной сети.");
    }

    return withSlash;
  }

  function requiresHttps(host) {
    const normalized = String(host || "").replace(/^\[/, "").replace(/\]$/, "").toLowerCase();
    if (normalized === "localhost") return false;
    if (normalized === "::1") return false;
    if (normalized.startsWith("fe80:")) return false;
    if (normalized.startsWith("fc") || normalized.startsWith("fd")) return false;
    if (normalized.endsWith(".local") || normalized.endsWith(".lan")) return false;
    if (!normalized.includes(".")) return false;

    const privateIpv4Pattern = /^(10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3})$/;
    if (privateIpv4Pattern.test(normalized)) return false;
    return true;
  }

  function normalizeToken(token) {
    return String(token || "").trim();
  }

  function getChecklistsSortedByUpdatedAt() {
    return [...state.checklists].sort((first, second) => {
      const byUpdatedAt = (Number(second.updatedAt) || 0) - (Number(first.updatedAt) || 0);
      if (byUpdatedAt !== 0) return byUpdatedAt;
      return second.id - first.id;
    });
  }

  function getChecklistById(checklistId) {
    return state.checklists.find((item) => item.id === checklistId) || null;
  }

  function compareItemsByPosition(first, second) {
    if (first.position !== second.position) {
      return first.position - second.position;
    }
    return first.id - second.id;
  }

  function isItemHiddenInHideMode(checklist, itemId) {
    const key = String(itemId);
    return Boolean(checklist.progress[key] && checklist.progress[key].hiddenInHideMode);
  }

  function isItemMarkedInMarkerMode(checklist, itemId) {
    const key = String(itemId);
    return Boolean(checklist.progress[key] && checklist.progress[key].markedInMarkerMode);
  }

  function isItemCompleted(checklist, itemId, mode) {
    if (mode === CHECKLIST_MODES.HIDE_ON_TAP) {
      return isItemHiddenInHideMode(checklist, itemId);
    }
    return isItemMarkedInMarkerMode(checklist, itemId);
  }

  function nextChecklistId() {
    state.seq.checklistId += 1;
    return state.seq.checklistId;
  }

  function nextItemId() {
    state.seq.itemId += 1;
    return state.seq.itemId;
  }

  function nowMs() {
    return Date.now();
  }

  function applyTheme() {
    const mode = state.settings.themeMode;
    if (mode === THEME_MODES.LIGHT) {
      document.documentElement.dataset.theme = "light";
      return;
    }
    if (mode === THEME_MODES.DARK) {
      document.documentElement.dataset.theme = "dark";
      return;
    }

    const prefersDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    document.documentElement.dataset.theme = prefersDark ? "dark" : "light";
  }

  function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.remove("hidden");

    if (ui.toastTimer !== null) {
      window.clearTimeout(ui.toastTimer);
    }
    ui.toastTimer = window.setTimeout(() => {
      elements.toast.classList.add("hidden");
      ui.toastTimer = null;
    }, TOAST_DURATION_MS);
  }

  function registerServiceWorker() {
    if (!("serviceWorker" in navigator)) return;
    window.addEventListener("load", () => {
      navigator.serviceWorker.register("./sw.js").catch(() => {
        // If the app is opened via plain HTTP from a non-local host,
        // Safari may reject service worker registration.
      });
    });
  }

  function loadState() {
    const fallback = createInitialState();
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return fallback;

    const parsed = safeParseJson(raw);
    if (!parsed || typeof parsed !== "object") return fallback;

    try {
      return sanitizeState(parsed);
    } catch (error) {
      return fallback;
    }
  }

  function createInitialState() {
    return {
      version: 1,
      settings: {
        themeMode: THEME_MODES.SYSTEM,
        checklistMode: CHECKLIST_MODES.HIDE_ON_TAP,
        serverBaseUrl: "https://example.com/api/",
        readToken: "",
        writeToken: "",
      },
      checklists: [],
      seq: {
        checklistId: 0,
        itemId: 0,
      },
    };
  }

  function sanitizeState(rawState) {
    const sanitized = createInitialState();
    const rawSettings = rawState.settings && typeof rawState.settings === "object"
      ? rawState.settings
      : {};
    sanitized.settings.themeMode = normalizeThemeMode(rawSettings.themeMode);
    sanitized.settings.checklistMode = normalizeChecklistMode(rawSettings.checklistMode);

    try {
      sanitized.settings.serverBaseUrl = normalizeBaseUrl(rawSettings.serverBaseUrl);
    } catch (error) {
      sanitized.settings.serverBaseUrl = "https://example.com/api/";
    }
    sanitized.settings.readToken = normalizeToken(rawSettings.readToken);
    sanitized.settings.writeToken = normalizeToken(rawSettings.writeToken);

    let maxChecklistId = 0;
    let maxItemId = 0;

    const rawChecklists = Array.isArray(rawState.checklists) ? rawState.checklists : [];
    rawChecklists.forEach((rawChecklist) => {
      if (!rawChecklist || typeof rawChecklist !== "object") return;

      const numericChecklistId = Number.parseInt(rawChecklist.id, 10);
      const checklistId = Number.isFinite(numericChecklistId) && numericChecklistId > 0
        ? numericChecklistId
        : maxChecklistId + 1;
      maxChecklistId = Math.max(maxChecklistId, checklistId);

      const rawItems = Array.isArray(rawChecklist.items) ? rawChecklist.items : [];
      const items = [];
      rawItems.forEach((rawItem, index) => {
        if (!rawItem || typeof rawItem !== "object") return;
        const itemName = String(rawItem.name || "").trim();
        if (itemName === "") return;
        const numericItemId = Number.parseInt(rawItem.id, 10);
        const itemId = Number.isFinite(numericItemId) && numericItemId > 0
          ? numericItemId
          : maxItemId + 1;
        maxItemId = Math.max(maxItemId, itemId);

        const numericPosition = Number.parseInt(rawItem.position, 10);
        const position = Number.isFinite(numericPosition) ? numericPosition : index;
        items.push({
          id: itemId,
          name: itemName,
          position,
        });
      });

      const progress = {};
      if (rawChecklist.progress && typeof rawChecklist.progress === "object") {
        Object.keys(rawChecklist.progress).forEach((key) => {
          const numericKey = Number.parseInt(key, 10);
          if (!Number.isFinite(numericKey)) return;
          const value = rawChecklist.progress[key];
          if (!value || typeof value !== "object") return;
          progress[String(numericKey)] = {
            hiddenInHideMode: Boolean(value.hiddenInHideMode),
            markedInMarkerMode: Boolean(value.markedInMarkerMode),
          };
        });
      }

      const title = String(rawChecklist.title || "").trim();
      const updatedAtNumber = Number(rawChecklist.updatedAt);
      const updatedAt = Number.isFinite(updatedAtNumber) ? updatedAtNumber : nowMs();

      sanitized.checklists.push({
        id: checklistId,
        serverId: String(rawChecklist.serverId || "").trim() || null,
        title,
        isFromServer: Boolean(rawChecklist.isFromServer),
        updatedAt,
        items,
        progress,
      });
    });

    const rawSeq = rawState.seq && typeof rawState.seq === "object" ? rawState.seq : {};
    sanitized.seq.checklistId = Math.max(maxChecklistId, Number.parseInt(rawSeq.checklistId, 10) || 0);
    sanitized.seq.itemId = Math.max(maxItemId, Number.parseInt(rawSeq.itemId, 10) || 0);

    return sanitized;
  }

  function normalizeThemeMode(rawMode) {
    if (rawMode === THEME_MODES.LIGHT) return THEME_MODES.LIGHT;
    if (rawMode === THEME_MODES.DARK) return THEME_MODES.DARK;
    return THEME_MODES.SYSTEM;
  }

  function normalizeChecklistMode(rawMode) {
    if (rawMode === CHECKLIST_MODES.MARKER) return CHECKLIST_MODES.MARKER;
    return CHECKLIST_MODES.HIDE_ON_TAP;
  }

  function saveState() {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }

  function generateUuid() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
      return window.crypto.randomUUID();
    }
    const random = Math.random().toString(16).slice(2);
    return "local-" + nowMs() + "-" + random;
  }

  function safeParseJson(raw) {
    try {
      return JSON.parse(raw);
    } catch (error) {
      return null;
    }
  }

  function escapeHtml(text) {
    return String(text)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }
})();
