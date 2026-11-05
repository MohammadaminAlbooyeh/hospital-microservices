{{/*
Common name for the chart.
*/}}
{{- define "hospital-platform.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{/*
Common labels shared by all resources.
*/}}
{{- define "hospital-platform.labels" -}}
app.kubernetes.io/part-of: hospital-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Labels for a specific microservice (name passed in as .).
*/}}
{{- define "hospital-platform.serviceLabels" -}}
app: {{ . }}
tier: service
app.kubernetes.io/part-of: hospital-platform
{{- end -}}

{{/*
Resolve the image repository:tag for a service, falling back to global defaults.
Usage: include "hospital-platform.image" (dict "svc" $svc "global" .Values.global)
*/}}
{{- define "hospital-platform.image" -}}
{{- $repo := .svc.image.repository -}}
{{- $tag := .svc.image.tag | default .global.imageTag -}}
{{- printf "%s:%s" $repo $tag -}}
{{- end -}}
