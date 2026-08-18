import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Navigate } from "react-router-dom";
import { useFeature } from "../hooks/useFeature";
import {
  fetchLlmProviderHealth,
  fetchLlmProviders,
  fetchLlmRoutes,
  fetchMcpAuditFilterOptions,
  fetchMcpAuditLogs,
  upsertLlmProvider,
  upsertLlmRoute,
  type McpAuditLogItem,
  type LlmProvider,
  type LlmProviderPayload,
  type ProviderType,
  type WorkloadType,
} from "../services/llmAdminApi";
import { executeMcpTool, listMcpTools } from "../services/mcpApi";
import type { McpToolDescriptor } from "../types/mcp";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  CardFooter,
} from "../components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "../components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "../components/ui/dialog";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { Switch } from "../components/ui/switch";
import { Badge } from "../components/ui/badge";
import {
  Activity,
  Settings, 
  Server, 
  Zap, 
  Key, 
  RefreshCw, 
  AlertCircle, 
  CheckCircle2, 
  Plus, 
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  Clock,
  Database,
  Info,
  Lock,
  Cpu,
  Loader2,
  Play,
  Pause,
} from "lucide-react";
import { toast } from "sonner";
import { cn } from "../lib/utils";
import { ScrollArea } from "../components/ui/scroll-area";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "../components/ui/tooltip";
import { USE_MCP_CLIENT } from "../constants";
import { getAutoGenStatus, toggleAutoGen, configureAutoGen, getAutoGenLogs, type AutoGenStatus, type AutoGenLog } from "../services/autoGenApi";

const WORKLOADS: WorkloadType[] = ["COURSE_GENERATION", "LESSON_GENERATION", "AI_COACH"];

function JsonHighlighter({ json }: { json: string | null | undefined }) {
  if (!json) return <span className="italic text-muted-foreground/50">No response body captured for this record.</span>;
  
  let formatted = "";
  try {
    formatted = JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    formatted = json;
  }

  const highlighted = formatted
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g, (match) => {
      let cls = 'text-emerald-600 dark:text-emerald-400'; // default (numbers)
      if (/^"/.test(match)) {
        if (/:$/.test(match)) {
          cls = 'text-blue-600 dark:text-sky-300'; // key
        } else {
          cls = 'text-orange-600 dark:text-orange-400'; // string
        }
      } else if (/true|false/.test(match)) {
        cls = 'text-purple-600 dark:text-purple-400'; // boolean
      } else if (/null/.test(match)) {
        cls = 'text-gray-500 dark:text-gray-400'; // null
      }
      return `<span class="${cls}">${match}</span>`;
    });

  return (
    <pre 
      className="p-4 font-mono text-[11px] leading-relaxed whitespace-pre-wrap text-foreground/90" 
      dangerouslySetInnerHTML={{ __html: highlighted }} 
    />
  );
}

export default function LlmAdmin() {
  const adminFeature = useFeature("ADMIN_PANEL");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [healthPollStartedAt] = useState<number>(Date.now());
  const [lastHealthPollSuccessAt, setLastHealthPollSuccessAt] = useState<number | null>(null);
  const [healthClockTick, setHealthClockTick] = useState<number>(Date.now());
  const [providers, setProviders] = useState<LlmProvider[]>([]);
  const [routes, setRoutes] = useState<Record<WorkloadType, string>>({
    COURSE_GENERATION: "",
    LESSON_GENERATION: "",
    AI_COACH: "",
  });
  const [error, setError] = useState<string>("");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [activeTab, setActiveTab] = useState<"routing" | "providers" | "audit" | "tools" | "autogen">("routing");
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditItems, setAuditItems] = useState<McpAuditLogItem[]>([]);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditTotalItems, setAuditTotalItems] = useState(0);
  const [auditFilterOptions, setAuditFilterOptions] = useState<{ tools: string[]; statuses: string[] }>({
    tools: [],
    statuses: [],
  });
  const [auditFilters, setAuditFilters] = useState({
    tool: "",
    status: "",
    fromLocal: "",
    toLocal: "",
    page: 0,
    size: 20,
  });
  const [mcpTools, setMcpTools] = useState<McpToolDescriptor[]>([]);
  const [mcpToolsLoading, setMcpToolsLoading] = useState(false);
  const [executingTool, setExecutingTool] = useState<McpToolDescriptor | null>(null);
  const [toolInput, setToolInput] = useState<string>("{}");
  const [toolResult, setToolResult] = useState<any>(null);
  const [isExecuting, setIsExecuting] = useState(false);
  const [viewingAuditItem, setViewingAuditItem] = useState<McpAuditLogItem | null>(null);

  // --- Auto-generation state ---
  const [autoGenStatus, setAutoGenStatus] = useState<AutoGenStatus | null>(null);
  const [autoGenLoading, setAutoGenLoading] = useState(false);
  const [autoGenToggling, setAutoGenToggling] = useState(false);
  const [autoGenConfigSaving, setAutoGenConfigSaving] = useState(false);
  const [autoGenBatchSize, setAutoGenBatchSize] = useState(2);
  const [autoGenIntervalSec, setAutoGenIntervalSec] = useState(60);
  const [autoGenLogs, setAutoGenLogs] = useState<AutoGenLog[]>([]);
  const [autoGenPage, setAutoGenPage] = useState(0);
  const [autoGenTotalPages, setAutoGenTotalPages] = useState(0);
  const [autoGenTotalItems, setAutoGenTotalItems] = useState(0);
  const autoGenPageRef = useRef(0);
  autoGenPageRef.current = autoGenPage;
  const auditFiltersRef = useRef(auditFilters);

  const [form, setForm] = useState({
    code: "",
    displayName: "",
    providerType: "GEMINI" as ProviderType,
    modelName: "",
    baseUrl: "",
    keyCooldownHours: 24,
    enabled: true,
    apiKeysText: "",
  });

  const providerCodes = useMemo(() => providers.map((p) => p.code), [providers]);
  const staleReference = lastHealthPollSuccessAt ?? healthPollStartedAt;
  const isHealthStale = !loading && healthClockTick - staleReference > 45_000;

  useEffect(() => {
    auditFiltersRef.current = auditFilters;
  }, [auditFilters]);

  useEffect(() => {
    if (adminFeature.loading || !adminFeature.allowed) {
      return;
    }
    refreshAll();
  }, [adminFeature.loading, adminFeature.allowed]);

  useEffect(() => {
    if (adminFeature.loading || !adminFeature.allowed) {
      return;
    }

    let active = true;
    const pollHealth = async () => {
      setHealthClockTick(Date.now());
      try {
        const snapshots = await fetchLlmProviderHealth();
        if (!active) return;

        setProviders((prev) =>
          prev.map((provider) => {
            const snapshot = snapshots.find((entry) => entry.providerCode === provider.code);
            if (!snapshot) return provider;
            return {
              ...provider,
              activeKeyCount: snapshot.activeKeyCount,
              coolingDownKeyCount: snapshot.coolingDownKeyCount,
              lastError: snapshot.lastError ?? null,
              lastErrorAt: snapshot.lastErrorAt ?? null,
              lastSuccessAt: snapshot.lastSuccessAt ?? null,
            };
          })
        );
        const now = Date.now();
        setLastHealthPollSuccessAt(now);
        setHealthClockTick(now);
      } catch {
        // Keep polling silent
      }
    };

    const intervalId = window.setInterval(pollHealth, 15000);
    pollHealth();

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [adminFeature.loading, adminFeature.allowed]);

  async function refreshAll() {
    setLoading(true);
    setError("");
    try {
      const [providerList, routeList] = await Promise.all([fetchLlmProviders(), fetchLlmRoutes()]);
      setProviders(providerList);
      const mapped: Record<WorkloadType, string> = {
        COURSE_GENERATION: "",
        LESSON_GENERATION: "",
        AI_COACH: "",
      };
      routeList.forEach((r) => {
        mapped[r.workload] = r.providerCode;
      });
      setRoutes(mapped);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to load LLM settings";
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }

  // --- Auto-generation helpers ---
  const fetchAutoGenStatus = useCallback(async (page: number) => {
    try {
      const [s, res] = await Promise.all([getAutoGenStatus(), getAutoGenLogs(page, 10)]);
      setAutoGenStatus(s);
      setAutoGenBatchSize(s.batchSize);
      setAutoGenIntervalSec(Math.round(s.intervalMs / 1000));
      setAutoGenLogs(res.items || []);
      setAutoGenTotalPages(res.totalPages || 0);
      setAutoGenTotalItems(res.totalItems || 0);
    } catch {
      // silent — user may not be admin
    }
  }, []);

  useEffect(() => {
    if (adminFeature.loading || !adminFeature.allowed) return;
    fetchAutoGenStatus(autoGenPageRef.current);
    const id = window.setInterval(() => {
      fetchAutoGenStatus(autoGenPageRef.current);
    }, 10_000);
    return () => window.clearInterval(id);
  }, [adminFeature.loading, adminFeature.allowed, fetchAutoGenStatus]);

  async function handleAutoGenToggle(enabled: boolean) {
    setAutoGenToggling(true);
    try {
      await toggleAutoGen(enabled);
      toast.success(enabled ? "Auto-generation enabled" : "Auto-generation disabled");
      await fetchAutoGenStatus(autoGenPageRef.current);
    } catch (e: any) {
      toast.error(e?.message || "Failed to toggle auto-generation");
    } finally {
      setAutoGenToggling(false);
    }
  }

  async function handleAutoGenConfigSave() {
    setAutoGenConfigSaving(true);
    try {
      await configureAutoGen({
        batchSize: autoGenBatchSize,
        intervalMs: autoGenIntervalSec * 1000,
      });
      toast.success("Auto-generation configuration saved");
      await fetchAutoGenStatus(autoGenPageRef.current);
    } catch (e: any) {
      toast.error(e?.message || "Failed to save configuration");
    } finally {
      setAutoGenConfigSaving(false);
    }
  }

  function toIsoDate(value: string): string | undefined {
    if (!value) {
      return undefined;
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return undefined;
    }
    return parsed.toISOString();
  }

  const loadAuditFilterOptions = useCallback(async () => {
    try {
      const options = await fetchMcpAuditFilterOptions();
      setAuditFilterOptions({
        tools: Array.isArray(options?.tools) ? options.tools : [],
        statuses: Array.isArray(options?.statuses) ? options.statuses : [],
      });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to load audit filter options";
      toast.error(msg);
    }
  }, []);

  const loadAuditLogs = useCallback(async (filters: typeof auditFilters, nextPage?: number) => {
    setAuditLoading(true);
    try {
      const page = typeof nextPage === "number" ? nextPage : filters.page;
      const result = await fetchMcpAuditLogs({
        tool: filters.tool || undefined,
        status: filters.status || undefined,
        from: toIsoDate(filters.fromLocal),
        to: toIsoDate(filters.toLocal),
        page,
        size: filters.size,
      });
      setAuditItems(Array.isArray(result.items) ? result.items : []);
      setAuditTotalItems(result.totalItems ?? 0);
      setAuditTotalPages(result.totalPages ?? 0);
      setAuditFilters((prev) => ({ ...prev, page: result.page ?? page }));
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to load audit logs";
      toast.error(msg);
    } finally {
      setAuditLoading(false);
    }
  }, []);

  useEffect(() => {
    if (adminFeature.loading || !adminFeature.allowed) {
      return;
    }
    if (activeTab !== "audit") {
      return;
    }

    loadAuditFilterOptions();
    loadAuditLogs(auditFiltersRef.current);
  }, [adminFeature.loading, adminFeature.allowed, activeTab, loadAuditFilterOptions, loadAuditLogs]);

  useEffect(() => {
    if (adminFeature.loading || !adminFeature.allowed || activeTab !== "tools") {
      return;
    }

    const fetchTools = async () => {
      setMcpToolsLoading(true);
      try {
        const tools = await listMcpTools();
        setMcpTools(tools);
      } catch (e) {
        toast.error("Failed to fetch MCP tools");
      } finally {
        setMcpToolsLoading(false);
      }
    };

    fetchTools();
  }, [adminFeature.loading, adminFeature.allowed, activeTab]);

  function applyAuditFilters() {
    const nextFilters = { ...auditFilters, page: 0 };
    setAuditFilters(nextFilters);
    loadAuditLogs(nextFilters, 0);
  }

  async function saveProvider() {
    setSaving(true);
    setError("");
    try {
      const apiKeys = form.apiKeysText
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean);

      const payload: LlmProviderPayload = {
        code: form.code,
        displayName: form.displayName,
        providerType: form.providerType,
        modelName: form.modelName,
        baseUrl: form.baseUrl || undefined,
        keyCooldownHours: form.keyCooldownHours,
        enabled: form.enabled,
      };

      if (apiKeys.length > 0) {
        payload.apiKeys = apiKeys;
      } else if (!isEditing) {
        payload.apiKeys = [];
      }

      await upsertLlmProvider(payload);
      
      toast.success(`Provider "${form.displayName}" ${isEditing ? "updated" : "created"} successfully`);
      await refreshAll();
      setIsDialogOpen(false);
      resetForm();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to save provider";
      setError(msg);
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  async function saveRoute(workload: WorkloadType, providerCode: string) {
    if (!providerCode) return;
    setSaving(true);
    try {
      await upsertLlmRoute(workload, providerCode);
      setRoutes(prev => ({ ...prev, [workload]: providerCode }));
      toast.success(`Route for ${workload.replace("_", " ")} updated`);
    } catch (e) {
      toast.error("Failed to update route");
    } finally {
      setSaving(false);
    }
  }

  async function handleExecuteTool() {
    if (!executingTool) return;
    setIsExecuting(true);
    setToolResult(null);
    try {
      const parsedInput = JSON.parse(toolInput);
      const res = await executeMcpTool({
        tool: executingTool.name,
        input: parsedInput
      });
      setToolResult(res);
      if (res.success) {
        toast.success(`Tool ${executingTool.name} executed successfully`);
      } else {
        toast.error(`Tool execution failed: ${res.error}`);
      }
    } catch (e) {
      toast.error("Invalid JSON input or execution error");
      setToolResult({ success: false, error: e instanceof Error ? e.message : "Client-side error" });
    } finally {
      setIsExecuting(false);
    }
  }

  function resetForm() {
    setForm({
      code: "",
      displayName: "",
      providerType: "GEMINI",
      modelName: "",
      baseUrl: "",
      keyCooldownHours: 24,
      enabled: true,
      apiKeysText: "",
    });
    setIsEditing(false);
  }

  function handleEditProvider(provider: LlmProvider) {
    setForm({
      code: provider.code,
      displayName: provider.displayName,
      providerType: provider.providerType,
      modelName: provider.modelName,
      baseUrl: provider.baseUrl || "",
      keyCooldownHours: provider.keyCooldownHours,
      enabled: provider.enabled,
      apiKeysText: "", 
    });
    setIsEditing(true);
    setIsDialogOpen(true);
  }

  if (adminFeature.loading) {
    return (
      <div className="flex h-[80vh] items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <RefreshCw className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm font-medium text-muted-foreground animate-pulse">Verifying Admin Access...</p>
        </div>
      </div>
    );
  }

  if (!adminFeature.allowed) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="min-h-screen bg-transparent p-4 md:p-8 space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-8 h-8 text-primary" />
            <h1 className="text-3xl font-bold tracking-tight">LLM Operations</h1>
          </div>
          <p className="text-muted-foreground text-lg">
            Manage provider pools, key health, and workload routing.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className={cn(
            "flex items-center gap-1.5 px-3 py-1.5 rounded-full border bg-background/50 text-xs font-medium transition-colors duration-500",
            isHealthStale ? "text-amber-500 border-amber-500/30" : "text-emerald-500 border-emerald-500/30"
          )}>
            <div className={cn(
              "w-2 h-2 rounded-full",
              isHealthStale ? "bg-amber-500" : "bg-emerald-500",
              !isHealthStale && "animate-pulse"
            )} />
            {isHealthStale ? "Health Stale" : "System Healthy"}
          </div>
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border bg-background/50 text-xs font-medium text-muted-foreground">
            <Database className="w-3.5 h-3.5" />
            Transport: {USE_MCP_CLIENT ? "MCP" : "Legacy"}
          </div>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={refreshAll} 
            disabled={loading}
            className="shadow-sm hover:shadow-md transition-all active:scale-95"
          >
            <RefreshCw className={cn("w-4 h-4 mr-2", loading && "animate-spin")} />
            Refresh
          </Button>
        </div>
      </div>

      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as "routing" | "providers" | "audit")} className="w-full">
        <div className="flex items-center justify-between mb-2">
          <TabsList className="bg-muted/50 p-1 border">
            <TabsTrigger value="routing" className="gap-2">
              <Zap className="w-4 h-4" />
              Intelligence Routing
            </TabsTrigger>
            <TabsTrigger value="providers" className="gap-2">
              <Server className="w-4 h-4" />
              Provider Registry
            </TabsTrigger>
            <TabsTrigger value="tools" className="gap-2">
              <Zap className="w-4 h-4" />
              MCP Tool Registry
            </TabsTrigger>
            <TabsTrigger value="audit" className="gap-2">
              <Activity className="w-4 h-4" />
              Audit Logs
            </TabsTrigger>
            <TabsTrigger value="autogen" className="gap-2">
              <Cpu className="w-4 h-4" />
              Auto Generation
              {autoGenStatus?.enabled && (
                <span className="ml-1 w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              )}
            </TabsTrigger>
          </TabsList>
        </div>

        <TabsContent value="routing" className="space-y-6 mt-0">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {WORKLOADS.map((workload) => {
              const currentProvider = providers.find(p => p.code === routes[workload]);
              return (
                <Card key={workload} className="overflow-hidden border-border/60 hover:border-primary/50 transition-colors group">
                  <div className="h-1 bg-gradient-to-r from-primary/80 to-primary/20" />
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between mb-2">
                      <Badge variant="outline" className="text-[10px] uppercase tracking-wider font-bold">Workload</Badge>
                      <Database className="w-4 h-4 text-muted-foreground/50 group-hover:text-primary/50 transition-colors" />
                    </div>
                    <CardTitle className="text-xl capitalize">{workload.replace("_", " ").toLowerCase()}</CardTitle>
                    <CardDescription>Target provider for this task</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="p-3 rounded-lg bg-muted/40 border border-border/40 space-y-2">
                      <Label className="text-[11px] text-muted-foreground uppercase">Active Provider</Label>
                      <Select 
                        value={routes[workload] || "_none"} 
                        onValueChange={(val) => saveRoute(workload, val === "_none" ? "" : val)}
                        disabled={loading || saving}
                      >
                        <SelectTrigger className="w-full bg-muted/50 border-border font-medium">
                          <SelectValue placeholder="Select provider" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="_none" className="text-muted-foreground">Select provider</SelectItem>
                          {providers.filter(p => p.enabled).map((p) => (
                            <SelectItem key={p.code} value={p.code}>
                              <div className="flex items-center gap-2">
                                <span className="font-semibold">{p.displayName}</span>
                                <span className="text-[11px] text-muted-foreground">({p.modelName})</span>
                              </div>
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    {currentProvider && (
                      <div className="flex items-center gap-4 px-1">
                        <div className="text-center">
                          <div className="text-lg font-bold text-primary">{currentProvider.activeKeyCount}</div>
                          <div className="text-[10px] text-muted-foreground uppercase font-medium">Ready Keys</div>
                        </div>
                        <div className="w-px h-8 bg-border" />
                        <div className="text-center">
                          <div className="text-lg font-bold text-amber-500">{currentProvider.coolingDownKeyCount}</div>
                          <div className="text-[10px] text-muted-foreground uppercase font-medium">Cooldown</div>
                        </div>
                        <div className="ml-auto">
                          {currentProvider.lastSuccessAt ? (
                            <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                          ) : (
                            <AlertCircle className="w-5 h-5 text-muted-foreground/30" />
                          )}
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </TabsContent>

        <TabsContent value="providers" className="space-y-6 mt-0">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold flex items-center gap-2 text-foreground/80">
              <Database className="w-5 h-5" />
              Configured Pools
              <Badge variant="secondary" className="ml-2 bg-muted text-muted-foreground">{providers.length}</Badge>
            </h2>
            <Dialog open={isDialogOpen} onOpenChange={(open) => {
               setIsDialogOpen(open);
               if (!open) resetForm();
            }}>
              <DialogTrigger asChild>
                <Button size="sm" className="gap-2 shadow-lg shadow-primary/20">
                  <Plus className="w-4 h-4" />
                  Register Provider
                </Button>
              </DialogTrigger>
              <DialogContent className="max-w-xl max-h-[85vh] p-0 flex flex-col border-border/80 shadow-2xl backdrop-blur-md overflow-hidden">
                <DialogHeader className="p-8 pb-4 bg-muted/10 border-b relative">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-primary/10 text-primary shrink-0">
                      <Server className="w-6 h-6" />
                    </div>
                    <div>
                      <DialogTitle className="text-2xl font-bold tracking-tight text-foreground">
                        {isEditing ? "Update Provider Pool" : "Register New Provider"}
                      </DialogTitle>
                      <DialogDescription className="text-[13px] mt-1 text-muted-foreground font-medium italic">
                        Configure architecture specs and key rotation parameters.
                      </DialogDescription>
                    </div>
                  </div>
                </DialogHeader>
                
                <div className="flex-1 overflow-y-auto px-8 py-10 space-y-12">
                  <div className="space-y-5">
                    <div className="flex items-center gap-2 border-b border-border/40 pb-3">
                      <span className="p-1 rounded bg-primary/10">
                         <Info className="w-4 h-4 text-primary" />
                      </span>
                      <h4 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Provider Identity Info</h4>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
                      <div className="space-y-2">
                        <Label htmlFor="code" className="text-[12px] font-bold text-foreground/70 uppercase">Immutable System Code</Label>
                        <Input 
                          id="code" 
                          placeholder="e.g. vertex-prod-01" 
                          className="bg-background h-10 border-border/60 font-mono shadow-inner text-foreground/80"
                          value={form.code} 
                          onChange={e => setForm(f => ({ ...f, code: e.target.value }))}
                          disabled={isEditing}
                        />
                        <p className="text-[10px] text-muted-foreground/60 px-1 font-bold italic">Critical system identifier for routing architecture.</p>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="displayName" className="text-[12px] font-bold text-foreground/70 uppercase">Dashboard Public Name</Label>
                        <Input 
                          id="displayName" 
                          placeholder="e.g. Google Vertex AI" 
                          className="bg-background h-10 border-border/60 shadow-sm"
                          value={form.displayName} 
                          onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="space-y-5">
                    <div className="flex items-center gap-2 border-b border-border/40 pb-3 font-semibold">
                      <Zap className="w-4 h-4 text-amber-500" />
                      <h4 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Technical Specification</h4>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
                      <div className="space-y-2">
                        <Label className="text-[12px] font-bold text-foreground/70 uppercase">Infrastructure Layer</Label>
                        <Select 
                          value={form.providerType} 
                          onValueChange={v => setForm(f => ({ ...f, providerType: v as ProviderType }))}
                        >
                          <SelectTrigger className="bg-background h-10 border-border/60">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="GEMINI" className="py-2.5">
                              <div className="flex items-center gap-2">
                                <Activity className="w-3.5 h-3.5 text-blue-500" />
                                <span className="font-semibold">Google Vertex / AI Studio</span>
                              </div>
                            </SelectItem>
                            <SelectItem value="GROQ" className="py-2.5">
                              <div className="flex items-center gap-2">
                                <Zap className="w-3.5 h-3.5 text-orange-500" />
                                <span className="font-semibold">Groq LPU Inference</span>
                              </div>
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="modelName" className="text-[12px] font-bold text-foreground/70 uppercase tracking-tight">Intelligence Engine ID</Label>
                        <Input 
                          id="modelName" 
                          placeholder="e.g. gemini-1.5-pro-latest" 
                          className="bg-background h-10 font-mono text-[12px] border-border/60 shadow-sm"
                          value={form.modelName} 
                          onChange={e => setForm(f => ({ ...f, modelName: e.target.value }))}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="baseUrl" className="text-[12px] font-bold text-foreground/70 uppercase">Gateway Override (URL)</Label>
                        <Input 
                          id="baseUrl" 
                          placeholder="Default System Endpoint" 
                          className="bg-background h-10 text-[12px] border-border/60 shadow-sm"
                          value={form.baseUrl} 
                          onChange={e => setForm(f => ({ ...f, baseUrl: e.target.value }))}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="cooldown" className="text-[12px] font-bold text-foreground/70 uppercase">Recovery Cooldown (Hrs)</Label>
                        <div className="relative">
                          <Input 
                            id="cooldown" 
                            type="number" 
                            className="bg-background h-10 pr-12 border-border/60 shadow-sm font-mono text-[13px]"
                            value={form.keyCooldownHours} 
                            onChange={e => setForm(f => ({ ...f, keyCooldownHours: parseInt(e.target.value) }))}
                          />
                          <Clock className="w-4 h-4 absolute right-4 top-1/2 -translate-y-1/2 text-muted-foreground/20" />
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between p-5 rounded-2xl border bg-muted/30 mt-6 group hover:border-primary/20 transition-all duration-300 shadow-sm">
                      <div className="space-y-0.5">
                        <Label className="text-[14px] font-bold tracking-tight group-hover:text-primary transition-colors cursor-pointer">Intelligence Routing Status</Label>
                        <p className="text-[12px] text-muted-foreground font-medium">Include this provider in the global workload balancing pool</p>
                      </div>
                      <Switch 
                        checked={form.enabled} 
                        onCheckedChange={checked => setForm(f => ({ ...f, enabled: checked }))} 
                        className="scale-110"
                      />
                    </div>
                  </div>

                  <div className="space-y-5">
                    <div className="flex items-center justify-between border-b border-border/40 pb-3">
                      <div className="flex items-center gap-2">
                        <Key className="w-4 h-4 text-emerald-500" />
                        <h4 className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Authentication Resource Pool</h4>
                      </div>
                      <Badge variant="outline" className="text-[10px] font-black h-4 px-2 bg-emerald-500/5 text-emerald-600 border-emerald-500/20">STRICT_ROTATION</Badge>
                    </div>
                    
                    <div className="space-y-4">
                      <div className="relative group">
                        <Textarea 
                          id="keys" 
                          className="min-h-[100px] font-mono text-[12px] p-4 bg-background border-border/60 outline-none focus-visible:ring-emerald-500/30 transition-all resize-none shadow-sm leading-relaxed" 
                          placeholder="ENTER_API_KEY_01&#10;ENTER_API_KEY_02&#10;..." 
                          value={form.apiKeysText}
                          onChange={e => setForm(f => ({ ...f, apiKeysText: e.target.value }))}
                        />
                        <div className="absolute top-4 right-4 text-emerald-500/5 group-focus-within:text-emerald-500/20 transition-colors pointer-events-none">
                          <Lock className="w-6 h-6" />
                        </div>
                      </div>
                      <div className="flex items-start gap-3 p-3 rounded-xl bg-amber-500/5 border border-amber-500/10 shadow-sm animate-in fade-in slide-in-from-top-1 duration-500">
                        <AlertCircle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
                        <div className="space-y-0.5">
                          <p className="text-[10px] text-amber-800 dark:text-amber-300 font-bold uppercase tracking-tight">Security & Persistence Policy:</p>
                          <p className="text-[10px] text-amber-700 dark:text-amber-400/80 leading-relaxed font-semibold">
                            Existing keys are protected. New entries will be merged into the active pool. 
                             One key per line.
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <DialogFooter className="p-8 border-t bg-muted/40 backdrop-blur-md flex items-center justify-between sm:justify-between w-full">
                  <div className="hidden sm:block shrink-0">
                    <p className="text-[10px] font-black text-muted-foreground/40 uppercase tracking-widest">OPS_CONTROL_ACTIVE</p>
                  </div>
                  <div className="flex items-center gap-4">
                    <Button variant="ghost" className="hover:bg-background h-10 px-5 font-bold text-xs uppercase tracking-tight" onClick={() => setIsDialogOpen(false)}>Cancel Action</Button>
                    <Button 
                      onClick={saveProvider} 
                      disabled={saving || !form.code || !form.displayName || !form.modelName}
                      className="min-w-[170px] h-10 font-black shadow-lg shadow-primary/20 bg-primary hover:bg-primary/90 text-[12px] uppercase tracking-wider flex items-center justify-center gap-2 group transition-all"
                    >
                      {saving ? (
                        <RefreshCw className="w-4 h-4 animate-spin" />
                      ) : (
                        <CheckCircle2 className="w-5 h-5 group-hover:scale-110 transition-transform" />
                      )}
                      {isEditing ? "Sync Pool" : "Register Pool"}
                    </Button>
                  </div>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {providers.map((p) => (
              <Card key={p.id} className={cn(
                "group relative overflow-hidden transition-all duration-300 hover:shadow-xl hover:-translate-y-1 border-border/60",
                !p.enabled && "opacity-80 grayscale-[0.5]"
              )}>
                <div className={cn(
                  "absolute top-0 right-0 w-24 h-24 -mr-8 -mt-8 rounded-full opacity-[0.03] group-hover:scale-150 transition-transform duration-700 blur-2xl",
                  p.enabled ? "bg-primary" : "bg-muted-foreground"
                )} />
                
                <CardHeader className="pb-3 border-b border-border/40 bg-muted/10">
                  <div className="flex items-start justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h3 className="font-bold text-lg leading-tight text-foreground">{p.displayName}</h3>
                        {!p.enabled && <Badge variant="secondary" className="h-4 text-[9px] uppercase px-1.5">Disabled</Badge>}
                      </div>
                      <div className="text-xs font-mono text-muted-foreground font-semibold tracking-tighter">{p.code}</div>
                    </div>
                    <Button 
                      variant="ghost" 
                      size="icon" 
                      className="h-8 w-8 rounded-full hover:bg-muted"
                      onClick={() => handleEditProvider(p)}
                    >
                      <Settings className="w-4 h-4 text-muted-foreground group-hover:text-primary transition-colors" />
                    </Button>
                  </div>
                </CardHeader>
                
                <CardContent className="pt-5 space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-widest">Technology</div>
                      <div className="flex items-center gap-1.5 font-bold text-sm text-foreground/80">
                        <Activity className="w-3.5 h-3.5 text-primary/70" />
                        {p.providerType}
                      </div>
                    </div>
                    <div className="space-y-1">
                      <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-widest">Core Model</div>
                      <div className="flex items-center gap-1.5 font-bold text-sm text-foreground/80">
                        <Zap className="w-3.5 h-3.5 text-amber-500/70" />
                        {p.modelName}
                      </div>
                    </div>
                  </div>

                  <div className="space-y-3 p-4 rounded-xl bg-muted/30 border border-border/40 shadow-inner">
                    <div className="flex items-center justify-between font-bold">
                      <div className="flex items-center gap-2">
                        <Key className="w-4 h-4 text-primary/60" />
                        <span className="text-xs uppercase tracking-tight">Key Distribution</span>
                      </div>
                      <span className="text-[11px] text-muted-foreground bg-background px-2 py-0.5 rounded-full border border-border/60">{p.keyCount} Total</span>
                    </div>
                    
                    <div className="space-y-2">
                      <div className="flex items-center justify-between text-[11px] font-bold">
                        <span className="text-emerald-600 uppercase tracking-tighter">Active Pool</span>
                        <span className="font-mono">{p.activeKeyCount} keys</span>
                      </div>
                      <div className="w-full h-2 bg-muted rounded-full overflow-hidden flex shadow-sm border border-border/20">
                        <div 
                          className="h-full bg-emerald-500 transition-all duration-1000 shadow-[0_0_8px_rgba(16,185,129,0.4)]"
                          style={{ width: `${(p.activeKeyCount / Math.max(p.keyCount, 1)) * 100}%` }}
                        />
                        <div 
                          className="h-full bg-amber-500 transition-all duration-1000"
                          style={{ width: `${(p.coolingDownKeyCount / Math.max(p.keyCount, 1)) * 100}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[11px] font-bold">
                        <span className="text-amber-600 uppercase tracking-tighter">Cooling Down</span>
                        <span className="font-mono">{p.coolingDownKeyCount} keys</span>
                      </div>
                    </div>
                  </div>

                  {p.lastError && (
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div className="flex items-center gap-2 p-2.5 rounded-xl border border-destructive/20 bg-destructive/5 text-destructive cursor-help group-hover:bg-destructive/10 transition-colors">
                            <AlertCircle className="w-4 h-4 shrink-0" />
                            <div className="text-[10px] font-black truncate uppercase tracking-tighter">System Outage Detected</div>
                          </div>
                        </TooltipTrigger>
                        <TooltipContent side="bottom" className="max-w-[320px] border-destructive bg-destructive text-destructive-foreground shadow-2xl p-4">
                          <div className="space-y-2">
                            <div className="flex items-center gap-2 font-black text-xs uppercase pb-2 border-b border-destructive-foreground/20">
                              <AlertCircle className="w-4 h-4" />
                              Critical Failure Report
                            </div>
                            <p className="text-[12px] leading-relaxed font-medium">{p.lastError}</p>
                            <p className="text-[10px] font-mono mt-2 pt-2 border-t border-destructive-foreground/20 italic opacity-80 flex items-center gap-2">
                              <Clock className="w-3 h-3" />
                              {p.lastErrorAt ? new Date(p.lastErrorAt).toLocaleString() : "Unknown Timestamp"}
                            </p>
                          </div>
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  )}
                </CardContent>

                <CardFooter className="pt-0 pb-5 flex flex-col gap-2">
                   <div className="w-full flex items-center justify-between text-[10px] font-bold text-muted-foreground/60 px-2 uppercase tracking-tight">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5" />
                        {p.lastSuccessAt ? `OK: ${new Date(p.lastSuccessAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit'})}` : "NO_OPERATIONS"}
                      </div>
                      <div className="flex items-center gap-1.5 group-hover:text-primary transition-colors cursor-pointer">
                        <Database className="w-3.5 h-3.5" />
                        {p.maskedKeys.length} KEYS LOADED
                      </div>
                   </div>
                </CardFooter>
              </Card>
            ))}

            {providers.length === 0 && !loading && (
              <div className="col-span-full py-24 flex flex-col items-center justify-center border-2 border-dashed rounded-[2rem] bg-muted/10 opacity-60 border-border/40">
                <div className="p-5 rounded-full bg-muted/20 mb-6">
                  <Server className="w-16 h-16 text-muted-foreground/30" />
                </div>
                <h3 className="text-2xl font-black text-foreground/70 uppercase tracking-tighter">Empty Provider Registry</h3>
                <p className="text-sm text-muted-foreground mt-2 font-medium">Initialize the system by registering your first LLM infrastructure pool.</p>
                <Button variant="outline" className="mt-8 h-12 px-8 font-black rounded-full shadow-lg hover:shadow-primary/10 transition-all border-primary/20 hover:bg-primary/5 group" onClick={() => setIsDialogOpen(true)}>
                  <Plus className="w-4 h-4 mr-2 group-hover:rotate-90 transition-transform" /> Register Environment Pool
                </Button>
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="tools" className="space-y-6 mt-0">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {mcpToolsLoading ? (
              <div className="col-span-full py-24 flex flex-col items-center justify-center border-2 border-dashed rounded-[2rem] bg-muted/10">
                <RefreshCw className="h-10 w-10 animate-spin text-primary/40" />
                <p className="mt-4 text-sm font-medium text-muted-foreground uppercase tracking-widest">Scanning RPC Interface...</p>
              </div>
            ) : mcpTools.length === 0 ? (
              <div className="col-span-full py-24 flex flex-col items-center justify-center border-2 border-dashed rounded-[2rem] bg-muted/10 opacity-60 border-border/40">
                <AlertCircle className="h-12 w-12 text-muted-foreground/30 mb-4" />
                <h3 className="text-xl font-bold uppercase tracking-tighter text-foreground/60">No Tools Registered</h3>
                <p className="text-sm text-muted-foreground mt-2">The MCP core reported 0 active tool capability descriptors.</p>
              </div>
            ) : (
              mcpTools.map((tool) => (
                <Card key={tool.name} className="border-border/60 hover:border-primary/40 transition-all duration-300 group">
                  <CardHeader className="pb-3 flex flex-row items-center justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <Badge variant="default" className="bg-primary/10 text-primary hover:bg-primary/20 border-primary/20 font-bold uppercase text-[10px]">Active</Badge>
                        <CardTitle className="text-lg font-mono">{tool.name}</CardTitle>
                      </div>
                      <CardDescription className="text-xs line-clamp-1">{tool.description}</CardDescription>
                    </div>
                    <div className="p-2 rounded-lg bg-muted group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                      <Zap className="h-5 w-5" />
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-4">
                     <div className="space-y-2">
                        <Label className="text-[10px] uppercase font-black text-muted-foreground/70 tracking-widest">Input Parameters</Label>
                        <div className="rounded-lg bg-muted/30 border border-border/40 p-4 font-mono text-[11px] min-h-[100px] whitespace-pre-wrap">
                          {JSON.stringify(tool.inputSchema, null, 2)}
                        </div>
                     </div>
                  </CardContent>
                  <CardFooter className="pt-0 pb-4 border-t border-border/40 bg-muted/5 flex items-center justify-between">
                    <div className="flex items-center gap-2 mt-4 text-[10px] font-bold text-muted-foreground/60 uppercase">
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                      Runtime Verified
                    </div>
                    <div className="flex items-center gap-2 mt-4">
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button 
                            variant="outline" 
                            size="sm" 
                            className="h-8 text-[10px] uppercase font-bold border-primary/20 hover:bg-primary/5"
                            onClick={() => {
                              setExecutingTool(tool);
                              setToolInput("{}");
                              setToolResult(null);
                            }}
                          >
                            Execute Tool <ChevronRight className="ml-1 w-3 h-3" />
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="max-w-2xl bg-background/95 backdrop-blur-lg">
                          <DialogHeader>
                            <DialogTitle className="font-mono">{tool.name}</DialogTitle>
                            <DialogDescription>{tool.description}</DialogDescription>
                          </DialogHeader>
                          <div className="space-y-4 py-4">
                            <div className="space-y-2">
                              <Label className="text-xs font-bold uppercase text-muted-foreground">JSON Payload</Label>
                              <Textarea 
                                className="min-h-[120px] font-mono text-xs bg-muted/50 border-border"
                                value={toolInput}
                                onChange={(e) => setToolInput(e.target.value)}
                                placeholder='{ "key": "value" }'
                              />
                            </div>
                            {toolResult && (
                              <div className={cn(
                                "flex flex-col rounded-lg border p-4 space-y-2 animate-in fade-in duration-300",
                                toolResult.success ? "bg-emerald-500/5 border-emerald-500/20" : "bg-destructive/5 border-destructive/20"
                              )}>
                                <div className="flex items-center justify-between">
                                  <span className="text-[10px] font-black uppercase">Execution Result</span>
                                  <Badge variant={toolResult.success ? "default" : "destructive"} className="text-[9px] h-4">
                                    {toolResult.success ? "SUCCESS" : "ERROR"}
                                  </Badge>
                                </div>
                                <pre className="text-[11px] font-mono whitespace-pre-wrap overflow-auto max-h-[200px]">
                                  {JSON.stringify(toolResult.data || toolResult.error, null, 2)}
                                </pre>
                              </div>
                            )}
                          </div>
                          <DialogFooter>
                            <Button variant="ghost" onClick={() => setExecutingTool(null)}>Close</Button>
                            <Button 
                              onClick={handleExecuteTool} 
                              disabled={isExecuting}
                              className="gap-2"
                            >
                              {isExecuting && <RefreshCw className="w-4 h-4 animate-spin" />}
                              Run Tool
                            </Button>
                          </DialogFooter>
                        </DialogContent>
                      </Dialog>
                    </div>
                  </CardFooter>
                </Card>
              ))
            )}
          </div>
        </TabsContent>

        <TabsContent value="audit" className="space-y-6 mt-0">
          <Card className="border-border/60">
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2">
                <Activity className="w-5 h-5" />
                MCP Audit Log Explorer
              </CardTitle>
              <CardDescription>
                Review MCP tool activity with filters by tool, status, and time range.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
                <div className="space-y-2">
                  <Label>Tool</Label>
                  <Select
                    value={auditFilters.tool || "__all__"}
                    onValueChange={(value) => setAuditFilters((prev) => ({ ...prev, tool: value === "__all__" ? "" : value }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All tools" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="__all__">All tools</SelectItem>
                      {auditFilterOptions.tools.map((tool) => (
                        <SelectItem key={tool} value={tool}>{tool}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Status</Label>
                  <Select
                    value={auditFilters.status || "__all__"}
                    onValueChange={(value) => setAuditFilters((prev) => ({ ...prev, status: value === "__all__" ? "" : value }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="__all__">All statuses</SelectItem>
                      {auditFilterOptions.statuses.map((status) => (
                        <SelectItem key={status} value={status}>{status}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>From</Label>
                  <Input
                    type="datetime-local"
                    value={auditFilters.fromLocal}
                    onChange={(e) => setAuditFilters((prev) => ({ ...prev, fromLocal: e.target.value }))}
                  />
                </div>

                <div className="space-y-2">
                  <Label>To</Label>
                  <Input
                    type="datetime-local"
                    value={auditFilters.toLocal}
                    onChange={(e) => setAuditFilters((prev) => ({ ...prev, toLocal: e.target.value }))}
                  />
                </div>

                <div className="flex items-end gap-2">
                  <Button onClick={applyAuditFilters} disabled={auditLoading} className="w-full">
                    {auditLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : "Apply"}
                  </Button>
                </div>
              </div>

              <div className="rounded-lg border border-border/60 overflow-hidden">
                <div className="overflow-x-auto">
                  <div className="min-w-[1320px]">
                    <div className="grid grid-cols-[220px_280px_130px_160px_160px_1fr_100px] gap-3 px-5 py-3 text-[11px] uppercase tracking-wider font-bold text-muted-foreground bg-muted/30">
                      <div>Time</div>
                      <div>Tool</div>
                      <div>Status</div>
                      <div className="whitespace-nowrap">User</div>
                      <div className="whitespace-nowrap">Latency</div>
                      <div>Result / Error</div>
                      <div className="text-right">Action</div>
                    </div>

                    <div className="max-h-[420px] overflow-y-auto">
                  {auditItems.length === 0 ? (
                    <div className="px-4 py-12 text-center text-sm text-muted-foreground">
                      {auditLoading ? "Loading audit logs..." : "No audit logs found for selected filters."}
                    </div>
                  ) : (
                    auditItems.map((item) => (
                      <div key={item.id} className="grid grid-cols-[220px_280px_130px_160px_160px_1fr_100px] gap-3 px-5 py-3 text-xs border-t border-border/40 hover:bg-muted/20 transition-colors items-center">
                        <div className="font-medium text-foreground/90 whitespace-nowrap">{new Date(item.createdAt).toLocaleString()}</div>
                        <div className="font-mono text-[11px] text-foreground/80 break-all">{item.tool}</div>
                        <div>
                          <Badge variant={item.status === "SUCCESS" ? "default" : "destructive"} className="text-[10px]">
                            {item.status}
                          </Badge>
                        </div>
                        <div className="text-foreground/80 whitespace-nowrap">{item.userId ?? "-"}</div>
                        <div className="text-foreground/80 whitespace-nowrap">{item.latencyMs != null ? `${item.latencyMs}ms` : "-"}</div>
                        <div className="text-muted-foreground break-words line-clamp-1">
                          {item.status === "SUCCESS" ? (
                            <span className="font-mono text-[10px] opacity-70">
                              {item.responseBody ? "Captured Response" : "No Content"}
                            </span>
                          ) : (
                            item.errorMessage || "-"
                          )}
                        </div>
                        <div className="text-right">
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="h-7 px-2 text-[10px] uppercase font-bold"
                            onClick={() => setViewingAuditItem(item)}
                          >
                            Details
                          </Button>
                        </div>
                      </div>
                    ))
                  )}
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between text-sm">
                <div className="text-muted-foreground">
                  {auditTotalItems} record{auditTotalItems === 1 ? "" : "s"} total
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => loadAuditLogs(auditFilters, Math.max(auditFilters.page - 1, 0))}
                    disabled={auditLoading || auditFilters.page <= 0}
                  >
                    Previous
                  </Button>
                  <span className="text-muted-foreground text-xs">
                    Page {auditFilters.page + 1} / {Math.max(auditTotalPages, 1)}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => loadAuditLogs(auditFilters, auditFilters.page + 1)}
                    disabled={auditLoading || auditFilters.page + 1 >= auditTotalPages}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* ────────── Auto Generation Tab ────────── */}
        <TabsContent value="autogen" className="space-y-6 mt-0">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Main Control Card */}
            <Card className="lg:col-span-2 overflow-hidden border-border/60">
              <div className={cn("h-1 bg-gradient-to-r", autoGenStatus?.enabled ? "from-green-500/80 to-emerald-500/40" : "from-muted-foreground/30 to-muted-foreground/10")} />
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={cn("p-2.5 rounded-xl", autoGenStatus?.enabled ? "bg-green-500/10" : "bg-muted/50")}>
                      <Cpu className={cn("w-5 h-5", autoGenStatus?.enabled ? "text-green-500" : "text-muted-foreground")} />
                    </div>
                    <div>
                      <CardTitle className="text-lg">Parallel Lesson Generation</CardTitle>
                      <CardDescription className="mt-1">
                        Automatically generate lesson content for all pending lessons in the background
                      </CardDescription>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    {autoGenStatus?.running && (
                      <Badge variant="outline" className="gap-1.5 text-amber-500 border-amber-500/30 bg-amber-500/5">
                        <Loader2 className="w-3 h-3 animate-spin" />
                        Running
                      </Badge>
                    )}
                    <Switch
                      checked={autoGenStatus?.enabled ?? false}
                      disabled={autoGenToggling}
                      onCheckedChange={handleAutoGenToggle}
                    />
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Status Banner */}
                <div className={cn(
                  "rounded-xl border p-4 flex items-center gap-4",
                  autoGenStatus?.enabled
                    ? "bg-green-500/5 border-green-500/20"
                    : "bg-muted/30 border-border/50"
                )}>
                  {autoGenStatus?.enabled ? (
                    <>
                      <Play className="w-5 h-5 text-green-500 shrink-0" />
                      <div>
                        <p className="text-sm font-semibold text-foreground">Auto-generation is active</p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          Processing {autoGenStatus?.batchSize ?? 2} lessons every {Math.round((autoGenStatus?.intervalMs ?? 60000) / 1000)}s.
                          {(autoGenStatus?.pendingLessons ?? 0) > 0 ? (
                            <> <span className="font-medium text-foreground">{autoGenStatus?.pendingLessons}</span> lessons pending.</>
                          ) : (
                            <> All lessons are generated! 🎉</>
                          )}
                        </p>
                      </div>
                    </>
                  ) : (
                    <>
                      <Pause className="w-5 h-5 text-muted-foreground shrink-0" />
                      <div>
                        <p className="text-sm font-semibold text-foreground">Auto-generation is paused</p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          Enable the toggle to start generating lesson content automatically.
                          {(autoGenStatus?.pendingLessons ?? 0) > 0 && (
                            <> <span className="font-medium text-amber-500">{autoGenStatus?.pendingLessons} lessons</span> waiting.</>
                          )}
                        </p>
                      </div>
                    </>
                  )}
                </div>

                {/* Configuration */}
                <div className="space-y-4">
                  <h4 className="text-xs font-bold uppercase tracking-widest text-muted-foreground flex items-center gap-2">
                    <Settings className="w-3.5 h-3.5" />
                    Configuration
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="ag-batch" className="text-xs font-medium">Batch Size (lessons per run)</Label>
                      <Select
                        value={String(autoGenBatchSize)}
                        onValueChange={(v) => setAutoGenBatchSize(Number(v))}
                      >
                        <SelectTrigger id="ag-batch" className="h-9">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {[1, 2, 3, 5, 10].map((n) => (
                            <SelectItem key={n} value={String(n)}>{n} lesson{n > 1 ? "s" : ""}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="ag-interval" className="text-xs font-medium">Interval (seconds between runs)</Label>
                      <Select
                        value={String(autoGenIntervalSec)}
                        onValueChange={(v) => setAutoGenIntervalSec(Number(v))}
                      >
                        <SelectTrigger id="ag-interval" className="h-9">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {[15, 30, 60, 120, 300].map((s) => (
                            <SelectItem key={s} value={String(s)}>
                              {s < 60 ? `${s}s` : `${s / 60}m`}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <div className="flex justify-end">
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={autoGenConfigSaving}
                      onClick={handleAutoGenConfigSave}
                      className="gap-2"
                    >
                      {autoGenConfigSaving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                      Save Configuration
                    </Button>
                  </div>
                </div>

                {/* Last Error */}
                {autoGenStatus?.lastError && (
                  <div className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 flex items-start gap-3">
                    <AlertCircle className="w-4 h-4 text-red-500 mt-0.5 shrink-0" />
                    <div>
                      <p className="text-xs font-semibold text-red-500">Last Error</p>
                      <p className="text-xs text-muted-foreground mt-1">{autoGenStatus.lastError}</p>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Stats Card */}
            <Card className="overflow-hidden border-border/60">
              <div className="h-1 bg-gradient-to-r from-blue-500/60 to-purple-500/30" />
              <CardHeader className="pb-3">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Activity className="w-4 h-4 text-blue-500" />
                  Generation Stats
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-xl bg-muted/30 border border-border/40 text-center">
                    <div className="text-2xl font-bold text-foreground">{autoGenStatus?.pendingLessons ?? "—"}</div>
                    <div className="text-[10px] uppercase font-bold text-muted-foreground mt-1">Pending</div>
                  </div>
                  <div className="p-3 rounded-xl bg-green-500/5 border border-green-500/20 text-center">
                    <div className="text-2xl font-bold text-green-500">{autoGenStatus?.totalGenerated ?? 0}</div>
                    <div className="text-[10px] uppercase font-bold text-muted-foreground mt-1">Generated</div>
                  </div>
                  <div className="p-3 rounded-xl bg-red-500/5 border border-red-500/20 text-center">
                    <div className="text-2xl font-bold text-red-500">{autoGenStatus?.totalFailed ?? 0}</div>
                    <div className="text-[10px] uppercase font-bold text-muted-foreground mt-1">Failed</div>
                  </div>
                  <div className="p-3 rounded-xl bg-muted/30 border border-border/40 text-center">
                    <div className="text-sm font-bold text-foreground">
                      {autoGenStatus?.lastRunTimestamp
                        ? new Date(autoGenStatus.lastRunTimestamp).toLocaleTimeString()
                        : "Never"}
                    </div>
                    <div className="text-[10px] uppercase font-bold text-muted-foreground mt-1">Last Run</div>
                  </div>
                </div>

                <div className="pt-3 border-t border-border/40 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Status</span>
                    <Badge variant="outline" className={cn(
                      "text-[10px]",
                      autoGenStatus?.enabled ? "text-green-500 border-green-500/30" : "text-muted-foreground"
                    )}>
                      {autoGenStatus?.enabled ? "Active" : "Inactive"}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Batch Size</span>
                    <span className="font-medium">{autoGenStatus?.batchSize ?? "—"}</span>
                  </div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-muted-foreground">Interval</span>
                    <span className="font-medium">{autoGenStatus ? `${Math.round(autoGenStatus.intervalMs / 1000)}s` : "—"}</span>
                  </div>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  className="w-full gap-2 mt-2"
                  onClick={() => fetchAutoGenStatus(autoGenPage)}
                  disabled={autoGenLoading}
                >
                  <RefreshCw className={cn("w-3.5 h-3.5", autoGenLoading && "animate-spin")} />
                  Refresh Stats
                </Button>
              </CardContent>
            </Card>
          </div>

          {/* Info Card */}
          <Card className="border-border/40 bg-muted/10">
            <CardContent className="py-4 flex items-start gap-3">
              <Info className="w-4 h-4 text-blue-500 mt-0.5 shrink-0" />
              <div className="text-xs text-muted-foreground space-y-1.5">
                <p><span className="font-semibold text-foreground">How it works:</span> When enabled, the scheduler picks up un-enriched lessons and calls the AI provider to generate content — just like clicking "Start" on a lesson, but automatically.</p>
                <p><span className="font-semibold text-foreground">API limits:</span> Adjust batch size and interval to stay within your AI provider's rate limits. Start with 1–2 lessons per 60s and increase once stable.</p>
                <p><span className="font-semibold text-foreground">Safe to toggle:</span> Disabling pauses the scheduler instantly. No in-flight generation is interrupted. Re-enabling resumes from where it left off.</p>
              </div>
            </CardContent>
          </Card>

          {/* Recent Activity Logs */}
          <Card className="border-border/60">
            <CardHeader className="pb-3 border-b border-border/40">
              <CardTitle className="text-lg flex items-center gap-2">
                <Activity className="w-5 h-5 text-primary" />
                Recent Generation Activity
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-muted/30 border-b border-border/60">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Time</th>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Course / Module</th>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Lesson</th>
                      <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/40">
                    {autoGenLogs.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-4 py-8 text-center text-muted-foreground">
                          No recent generation activity
                        </td>
                      </tr>
                    ) : (
                      autoGenLogs.map((log) => (
                        <tr key={`${log.lessonId}-${log.timestampMs}`} className="hover:bg-muted/10 transition-colors">
                          <td className="px-4 py-3 align-top whitespace-nowrap text-muted-foreground">
                            {new Date(log.timestampMs).toLocaleTimeString()}
                          </td>
                          <td className="px-4 py-3 align-top">
                            <div className="font-medium">{log.courseTitle}</div>
                            <div className="text-xs text-muted-foreground mt-0.5">{log.moduleTitle}</div>
                          </td>
                          <td className="px-4 py-3 align-top font-medium">
                            {log.lessonTitle}
                          </td>
                          <td className="px-4 py-3 align-top">
                            {log.success ? (
                              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-green-500/10 text-green-600 border border-green-500/20 text-xs font-semibold">
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                Success
                              </div>
                            ) : (
                              <div className="space-y-2">
                                <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-red-500/10 text-red-600 border border-red-500/20 text-xs font-semibold">
                                  <AlertCircle className="w-3.5 h-3.5" />
                                  Failed
                                </div>
                                {log.errorMessage && (
                                  <div className="text-[11px] text-red-500 bg-red-500/5 p-2 rounded border border-red-500/10 break-words max-w-md">
                                    {log.errorMessage}
                                  </div>
                                )}
                              </div>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination controls */}
              <div className="p-4 border-t border-border/40 flex items-center justify-between text-sm">
                <div className="text-muted-foreground">
                  {autoGenTotalItems} record{autoGenTotalItems === 1 ? "" : "s"} total
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const newPage = Math.max(autoGenPage - 1, 0);
                      setAutoGenPage(newPage);
                      fetchAutoGenStatus(newPage);
                    }}
                    disabled={autoGenPage <= 0}
                  >
                    Previous
                  </Button>
                  <span className="text-muted-foreground text-xs">
                    Page {autoGenPage + 1} / {Math.max(autoGenTotalPages, 1)}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const newPage = autoGenPage + 1;
                      setAutoGenPage(newPage);
                      fetchAutoGenStatus(newPage);
                    }}
                    disabled={autoGenPage + 1 >= autoGenTotalPages}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

      </Tabs>
      
      <Dialog open={!!viewingAuditItem} onOpenChange={(open) => !open && setViewingAuditItem(null)}>
        <DialogContent className="max-w-4xl max-h-[85vh] flex flex-col p-0 overflow-hidden border-border/40 shadow-2xl backdrop-blur-xl">
          <DialogHeader className="p-6 border-b bg-muted/20">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <DialogTitle className="flex items-center gap-2 font-mono text-lg">
                  <Activity className="w-5 h-5 text-primary" />
                  Audit Record: {viewingAuditItem?.requestId.split('-')[0]}...
                </DialogTitle>
                <DialogDescription className="text-xs uppercase font-bold tracking-widest text-muted-foreground">
                  Detailed Tool Execution Metadata
                </DialogDescription>
              </div>
              {viewingAuditItem && (
                <Badge variant={viewingAuditItem.status === "SUCCESS" ? "default" : "destructive"}>
                  {viewingAuditItem.status}
                </Badge>
              )}
            </div>
          </DialogHeader>

          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="p-3 rounded-xl bg-muted/30 border border-border/40">
                <div className="text-[10px] uppercase font-black text-muted-foreground/60 mb-1">Tool</div>
                <div className="font-mono text-xs font-bold truncate">{viewingAuditItem?.tool}</div>
              </div>
              <div className="p-3 rounded-xl bg-muted/30 border border-border/40">
                <div className="text-[10px] uppercase font-black text-muted-foreground/60 mb-1">Latency</div>
                <div className="font-mono text-xs font-bold">{viewingAuditItem?.latencyMs}ms</div>
              </div>
              <div className="p-3 rounded-xl bg-muted/30 border border-border/40">
                <div className="text-[10px] uppercase font-black text-muted-foreground/60 mb-1">User ID</div>
                <div className="font-mono text-xs font-bold">{viewingAuditItem?.userId ?? "N/A"}</div>
              </div>
              <div className="p-3 rounded-xl bg-muted/30 border border-border/40">
                <div className="text-[10px] uppercase font-black text-muted-foreground/60 mb-1">Role</div>
                <div className="font-mono text-xs font-bold">{viewingAuditItem?.userRole ?? "N/A"}</div>
              </div>
            </div>

            <div className="space-y-4">
              {viewingAuditItem?.errorMessage && (
                <div className="space-y-2">
                  <Label className="text-[10px] uppercase font-black text-destructive/80 flex items-center gap-1.5">
                    <AlertCircle className="w-3.5 h-3.5" />
                    Error Message
                  </Label>
                  <div className="p-4 rounded-xl bg-destructive/5 border border-destructive/20 text-destructive text-sm font-medium leading-relaxed">
                    {viewingAuditItem.errorMessage}
                  </div>
                </div>
              )}

              <div className="space-y-2">
                <Label className="text-[10px] uppercase font-black text-muted-foreground/70 flex items-center gap-1.5">
                  <Database className="w-3.5 h-3.5" />
                  Response Content
                </Label>
                <div className="rounded-xl bg-muted/50 border border-border/40 p-0 overflow-hidden">
                  <div className="flex items-center justify-between px-4 py-2 bg-muted/50 border-b border-border/20">
                    <span className="text-[10px] font-bold text-muted-foreground uppercase">JSON Payload</span>
                    <Button 
                      variant="ghost" 
                      size="sm" 
                      className="h-6 px-2 text-[10px] hover:bg-primary/10 hover:text-primary"
                      onClick={() => {
                        if (viewingAuditItem?.responseBody) {
                          navigator.clipboard.writeText(viewingAuditItem.responseBody);
                          toast.success("Copied to clipboard");
                        }
                      }}
                    >
                      Copy JSON
                    </Button>
                  </div>
                  <ScrollArea className="h-[450px] w-full">
                    <JsonHighlighter json={viewingAuditItem?.responseBody} />
                  </ScrollArea>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter className="p-4 border-t bg-muted/10">
            <Button variant="outline" className="h-9 px-6 font-bold text-xs uppercase" onClick={() => setViewingAuditItem(null)}>
              Dismiss Details
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <div className="fixed inset-0 pointer-events-none -z-10 bg-[radial-gradient(55%_45%_at_50%_40%,rgba(var(--primary-rgb),0.02)_0%,transparent_100%)]" />
    </div>
  );
}
