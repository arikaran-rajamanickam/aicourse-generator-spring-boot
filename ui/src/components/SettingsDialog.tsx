import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
} from "@/components/ui/dialog";
import { 
  User, 
  Settings, 
  Globe, 
  CreditCard, 
  Users as UsersIcon, 
  Sparkles,
  ShieldCheck,
  LogOut
} from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialTab?: string;
}

export function SettingsDialog({ open, onOpenChange, initialTab = "account" }: SettingsDialogProps) {
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState(initialTab);

  useEffect(() => {
    if (open) setActiveTab(initialTab);
  }, [open, initialTab]);

  if (!user) return null;

  const displayName = user.displayName ?? user.username ?? "User";
  const identifier = user.username ?? user.id ?? "user";
  const email = user.email;
  const avatarLetter = displayName[0]?.toUpperCase() ?? "U";

  const sidebarItems = [
    { id: "account",      label: "Account info", icon: User,      group: "User settings" },
    { id: "preferences",  label: "Preferences",  icon: Settings,  group: "User settings" },
    { id: "general",      label: "General",       icon: Globe,     group: "Workspace settings" },
    { id: "publishing",   label: "Publishing",    icon: Sparkles,  group: "Workspace settings", badge: true },
    { id: "billing",      label: "Billing",       icon: CreditCard, group: "Workspace settings" },
    { id: "users",        label: "Users",         icon: UsersIcon, group: "Workspace settings", badge: true },
  ];

  const groups = Array.from(new Set(sidebarItems.map(item => item.group)));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/*
        KEY FIX: DialogContent must have an explicit pixel height (not just vh)
        AND must NOT have overflow-hidden so its flex children can scroll.
        We set h-[700px] max to give a stable container for child overflow to work.
      */}
      <DialogContent
        className="max-w-[1000px] p-0 gap-0 bg-background border-border"
        style={{ height: "min(700px, 90vh)", display: "flex", flexDirection: "column", overflow: "hidden" }}
      >
        {/* flex row — fills the dialog exactly */}
        <div className="flex w-full" style={{ height: "100%", minHeight: 0 }}>

          {/* ── Sidebar ────────────────────────────────────── */}
          <aside className="w-64 border-r border-border bg-muted/20 hidden md:flex flex-col shrink-0" style={{ height: "100%", minHeight: 0 }}>
            {/* nav items — scrollable */}
            <div className="p-4 overflow-y-auto" style={{ flex: 1, minHeight: 0 }}>
              <div className="space-y-6">
                {groups.map(group => (
                  <div key={group} className="space-y-1">
                    <h3 className="px-2 text-[10px] font-black uppercase text-muted-foreground/60 tracking-widest mb-2">
                      {group}
                    </h3>
                    {sidebarItems.filter(item => item.group === group).map(item => (
                      <button
                        key={item.id}
                        onClick={() => setActiveTab(item.id)}
                        className={cn(
                          "w-full flex items-center gap-3 px-3 py-1.5 rounded-xl text-sm transition-all duration-200 group relative overflow-hidden",
                          activeTab === item.id
                            ? "bg-muted text-foreground font-bold shadow-sm"
                            : "text-muted-foreground font-semibold hover:bg-muted/80 hover:text-foreground"
                        )}
                      >
                        {activeTab === item.id && (
                          <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-4 bg-primary rounded-r-full" />
                        )}
                        <item.icon className={cn(
                          "w-4 h-4 transition-transform group-hover:scale-110",
                          activeTab === item.id ? "text-primary" : "text-muted-foreground/60 group-hover:text-foreground"
                        )} />
                        <span className="flex-1 text-left">{item.label}</span>
                        {item.badge && (
                          <div className="h-4 w-4 rounded bg-primary/20 flex items-center justify-center">
                            <Sparkles className="w-2.5 h-2.5 text-primary" />
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                ))}
              </div>
            </div>

            {/* logout pinned at bottom */}
            <div className="p-4 border-t border-border shrink-0">
              <Button
                variant="ghost"
                size="sm"
                className="w-full justify-start text-destructive hover:bg-destructive/10 hover:text-destructive gap-3 font-semibold"
                onClick={logout}
              >
                <LogOut className="w-4 h-4" />
                Log out
              </Button>
            </div>
          </aside>

          {/* ── Main content ───────────────────────────────── */}
          {/*
            KEY FIX: min-h-0 collapses the flex child below its content height
            so overflow-y-auto actually triggers.
          */}
          <main className="flex flex-col bg-background" style={{ flex: 1, minWidth: 0, height: "100%", minHeight: 0 }}>
            <div className="p-6 overflow-y-auto" style={{ flex: 1, minHeight: 0 }}>
              <div className="max-w-2xl mx-auto space-y-8">

                {/* ── Account tab ── */}
                {activeTab === "account" && (
                  <div className="space-y-8 animate-in fade-in slide-in-from-bottom-2 duration-300">
                    <div className="space-y-1">
                      <h3 className="text-2xl font-bold tracking-tight text-foreground">Account information</h3>
                      <p className="text-sm text-muted-foreground font-medium">Manage your profile details and private information.</p>
                    </div>

                    <Separator className="bg-border/60" />

                    {/* Profile Picture */}
                    <div className="space-y-4">
                      <Label className="text-sm font-bold">Profile picture</Label>
                      <div className="flex items-center gap-6">
                        <Avatar className="h-20 w-20 rounded-2xl border-2 border-background shadow-soft">
                          <AvatarImage src={user.profileImageUrl} />
                          <AvatarFallback className="bg-gradient-cta text-white text-2xl font-bold">
                            {avatarLetter}
                          </AvatarFallback>
                        </Avatar>
                        <div className="space-y-2">
                          <Button size="sm" variant="outline" className="font-bold rounded-lg border-border/50 hover:bg-muted/80">
                            Change picture
                          </Button>
                          <p className="text-[10px] text-muted-foreground uppercase font-black tracking-wider">
                            JPG, GIF or PNG. Max size 2MB.
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* Form Fields */}
                    <div className="grid gap-5">
                      <div className="space-y-2">
                        <Label className="text-sm font-bold flex items-center gap-2">
                          Email address <ShieldCheck className="w-3.5 h-3.5 text-primary" />
                        </Label>
                        <Input
                          defaultValue={email || ""}
                          disabled
                          placeholder="No email registered"
                          className="bg-white text-black border-none shadow-sm font-medium cursor-not-allowed opacity-80"
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm font-bold">First name</Label>
                          <Input
                            defaultValue={user.displayName?.split(" ")[0] ?? ""}
                            className="bg-white text-black border-none shadow-sm focus:ring-primary/20"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-bold">Last name</Label>
                          <Input
                            defaultValue={user.displayName?.split(" ").slice(1).join(" ") ?? ""}
                            placeholder="Your last name"
                            className="bg-white text-black border-none shadow-sm focus:ring-primary/20"
                          />
                        </div>
                      </div>
                    </div>

                    <Separator className="bg-border/60" />

                    {/* Password */}
                    <div className="space-y-4">
                      <h3 className="text-base font-bold">Password and authentication</h3>
                      <div className="p-5 rounded-xl border border-border/60 bg-muted/20 flex items-center justify-between hover:bg-muted/30 transition-colors">
                        <div className="space-y-1">
                          <p className="text-sm font-bold">Change password</p>
                          <p className="text-xs text-muted-foreground font-medium">Update your account password for better security.</p>
                        </div>
                        <Button variant="hero" size="sm" className="h-8 rounded-lg font-bold px-4 shrink-0 ml-4">
                          Change password
                        </Button>
                      </div>
                    </div>

                    <Separator className="bg-border/60" />

                    {/* Logout from all devices */}
                    <div className="space-y-4">
                      <h3 className="text-base font-bold text-destructive/80">Log out from all devices</h3>
                      <p className="text-xs text-muted-foreground font-medium leading-relaxed">
                        You will be logged out of all active sessions besides this one.
                      </p>
                      <Button variant="ghost" className="text-destructive font-bold hover:bg-destructive/10">
                        Log out everywhere
                      </Button>
                    </div>
                  </div>
                )}

                {/* ── Other tabs ── */}
                {activeTab !== "account" && (
                  <div className="flex flex-col items-center justify-center py-32 opacity-40">
                    <Settings className="w-12 h-12 mb-4 animate-pulse" />
                    <p className="font-black uppercase tracking-[0.2em] text-[10px]">Module Pending Implementation</p>
                  </div>
                )}

              </div>
            </div>
          </main>

        </div>
      </DialogContent>
    </Dialog>
  );
}
