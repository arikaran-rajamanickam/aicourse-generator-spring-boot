import { useState } from "react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useAuth } from "@/auth/AuthContext";
import { 
  User as UserIcon, 
  Settings as SettingsIcon, 
  LogOut as LogOutIcon, 
  ChevronRight as ChevronIcon,
  ShieldCheck as AdminIcon,
  Zap as CreditsIcon
} from "lucide-react";
import { cn } from "@/lib/utils";
import { SettingsDialog } from "./SettingsDialog";

export function ProfileDropdown() {
  const { user, logout } = useAuth();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [initialTab, setInitialTab] = useState("account");

  if (!user) return null;

  const displayName = user.displayName ?? user.username ?? "User";
  const identifier = user.username ?? user.id ?? "user";
  const email = user.email ?? `${identifier}@aicourse.gen`;
  const avatarLetter = displayName[0]?.toUpperCase() ?? "U";
  const isAdmin = user.roles?.includes("ADMIN");

  // Mocking credits info for premium feel
  const remainingCredits = 1093.44;
  const totalCredits = 2000;
  const percentage = (remainingCredits / totalCredits) * 100;

  const openSettings = (tab: string) => {
    setInitialTab(tab);
    setSettingsOpen(true);
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="relative h-9 w-9 rounded-xl overflow-hidden border border-border shadow-soft transition-all hover:ring-2 hover:ring-primary/20 group">
            <Avatar className="h-full w-full rounded-none">
              <AvatarImage src={user.profileImageUrl} alt={displayName} className="object-cover" />
              <AvatarFallback className="bg-gradient-cta text-white font-bold text-xs rounded-none">
                {avatarLetter}
              </AvatarFallback>
            </Avatar>
            <div className="absolute inset-0 bg-primary/0 group-hover:bg-primary/5 transition-colors" />
          </button>
        </DropdownMenuTrigger>
        
        <DropdownMenuContent 
          className="w-80 p-0 border-border bg-popover/90 backdrop-blur-xl shadow-soft overflow-hidden" 
          align="end" 
          sideOffset={12}
        >
          {/* Header Section */}
          <div className="p-5 bg-muted/40 border-b border-border/50">
            <div className="flex items-center gap-4">
              <Avatar className="h-12 w-12 rounded-xl border-2 border-background shadow-soft">
                <AvatarImage src={user.profileImageUrl} className="object-cover" />
                <AvatarFallback className="bg-gradient-cta text-white font-bold text-base">
                  {avatarLetter}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <h4 className="text-sm font-bold truncate tracking-tight text-foreground">{displayName}</h4>
                <p className="text-[11px] text-muted-foreground truncate font-medium">@{user.username || identifier}</p>
                {isAdmin && (
                  <div className="flex items-center gap-1 mt-1 text-[10px] font-black uppercase text-primary tracking-widest leading-none">
                    <AdminIcon className="w-3 h-3" />
                    System Admin
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className="p-2 space-y-1">
            <DropdownMenuGroup>
              <DropdownMenuItem 
                onClick={() => openSettings("account")}
                className="flex items-center gap-3 px-3 py-1.5 rounded-xl focus:bg-muted focus:text-accent-foreground transition-all cursor-pointer group hover:bg-muted/80 mb-0.5"
              >
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-primary group-hover:bg-primary/20 transition-colors">
                  <UserIcon className="h-3.5 w-3.5" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-bold text-foreground/90 group-hover:text-foreground">Account info</p>
                </div>
                <ChevronIcon className="h-3 w-3 text-muted-foreground/40 group-hover:text-muted-foreground/70 transition-colors" />
              </DropdownMenuItem>

              <DropdownMenuItem 
                onClick={() => openSettings("preferences")}
                className="flex items-center gap-3 px-3 py-1.5 rounded-xl focus:bg-muted focus:text-accent-foreground transition-all cursor-pointer group hover:bg-muted/80"
              >
                <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-muted text-muted-foreground group-hover:bg-muted/80 transition-colors">
                  <SettingsIcon className="h-3.5 w-3.5" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-bold text-foreground/90 group-hover:text-foreground">Preferences</p>
                </div>
                <ChevronIcon className="h-3 w-3 text-muted-foreground/40 group-hover:text-muted-foreground/70 transition-colors" />
              </DropdownMenuItem>
            </DropdownMenuGroup>

            <DropdownMenuSeparator className="mx-2 bg-border/40" />

            {/* Credits Section */}
            <div className="px-3 py-4 mx-1 my-1 rounded-xl bg-muted/20 border border-border/10">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/20 text-primary ring-4 ring-primary/5">
                    <CreditsIcon className="h-3 w-3 fill-current" />
                  </div>
                  <span className="text-xs font-black uppercase tracking-widest">Credits</span>
                </div>
                <Button size="sm" variant="secondary" className="h-6 px-2.5 text-[10px] font-bold uppercase tracking-wider rounded-full hover:bg-primary hover:text-white transition-all">
                  Upgrade
                </Button>
              </div>
              
              <Progress value={percentage} className="h-1.5 bg-background border border-border/20" />
              
              <div className="flex justify-between items-center mt-3">
                <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Remaining</span>
                <span className="text-sm font-mono font-bold tracking-tight">{remainingCredits.toLocaleString()}</span>
              </div>
            </div>

            <DropdownMenuSeparator className="mx-2 bg-border/40" />

            <DropdownMenuItem 
              onClick={logout}
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg focus:bg-destructive/10 focus:text-destructive transition-colors cursor-pointer"
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-destructive/10 text-destructive">
                <LogOutIcon className="h-4 w-4" />
              </div>
              <span className="text-sm font-semibold">Log out</span>
            </DropdownMenuItem>
          </div>
        </DropdownMenuContent>
      </DropdownMenu>

      <SettingsDialog 
        open={settingsOpen} 
        onOpenChange={setSettingsOpen} 
        initialTab={initialTab} 
      />
    </>
  );
}
