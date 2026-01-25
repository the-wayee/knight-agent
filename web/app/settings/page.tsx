"use client"

import Link from "next/link"
import { Header } from "@/components/layout/header"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { 
  Server, 
  Key, 
  Palette, 
  Bell,
  ChevronRight 
} from "lucide-react"

const settingsSections = [
  {
    title: "MCP Servers",
    description: "Manage connected MCP servers and tools",
    icon: Server,
    href: "/settings/mcp",
  },
  {
    title: "API Keys",
    description: "Configure API keys for LLM providers",
    icon: Key,
    href: "/settings/api-keys",
  },
  {
    title: "Appearance",
    description: "Customize the look and feel",
    icon: Palette,
    href: "/settings/appearance",
  },
  {
    title: "Notifications",
    description: "Configure notification preferences",
    icon: Bell,
    href: "/settings/notifications",
  },
]

export default function SettingsPage() {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-8">
          <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
          <p className="text-muted-foreground mt-1">
            Manage your account and application preferences
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          {settingsSections.map((section) => (
            <Link key={section.title} href={section.href}>
              <Card className="hover:border-primary/20 hover:shadow-md transition-all cursor-pointer h-full">
                <CardHeader className="pb-3">
                  <div className="flex items-center justify-between">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                      <section.icon className="h-5 w-5 text-muted-foreground" />
                    </div>
                    <ChevronRight className="h-5 w-5 text-muted-foreground" />
                  </div>
                </CardHeader>
                <CardContent>
                  <CardTitle className="text-base">{section.title}</CardTitle>
                  <CardDescription className="mt-1">{section.description}</CardDescription>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </main>
    </div>
  )
}
