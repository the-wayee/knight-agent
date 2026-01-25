export function formatDistanceToNow(date: string | Date): string {
  const now = new Date()
  const target = typeof date === "string" ? new Date(date) : date
  const diffMs = now.getTime() - target.getTime()
  
  const seconds = Math.floor(diffMs / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) {
    return days === 1 ? "1 day ago" : `${days} days ago`
  }
  if (hours > 0) {
    return hours === 1 ? "1 hour ago" : `${hours} hours ago`
  }
  if (minutes > 0) {
    return minutes === 1 ? "1 minute ago" : `${minutes} minutes ago`
  }
  return "just now"
}

export function formatDateTime(date: string | Date): string {
  const target = typeof date === "string" ? new Date(date) : date
  return target.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  })
}
