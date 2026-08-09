{{/*
Expand the name of the chart.
*/}}
{{- define "planner.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a fully qualified app name.
*/}}
{{- define "planner.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "planner.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "planner.labels" -}}
helm.sh/chart: {{ include "planner.chart" . }}
{{ include "planner.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels.
*/}}
{{- define "planner.selectorLabels" -}}
app.kubernetes.io/name: {{ include "planner.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Chart name and version.
*/}}
{{- define "planner.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Planner service name.
*/}}
{{- define "planner.plannerServiceName" -}}
{{- .Values.planner.service.name | default (printf "%s-planner-service" (include "planner.fullname" .)) -}}
{{- end }}

{{/*
MySQL service name.
*/}}
{{- define "planner.mysqlServiceName" -}}
{{- .Values.mysql.service.name | default (printf "%s-mysql-service" (include "planner.fullname" .)) -}}
{{- end }}

{{/*
Planner ConfigMap name.
*/}}
{{- define "planner.configMapName" -}}
{{- printf "%s-planner-config-map" (include "planner.fullname" .) -}}
{{- end }}

{{/*
Planner Secret name.
*/}}
{{- define "planner.secretName" -}}
{{- printf "%s-planner-secret" (include "planner.fullname" .) -}}
{{- end }}

{{/*
MySQL StatefulSet name.
*/}}
{{- define "planner.mysqlStatefulSetName" -}}
{{- printf "%s-mysql-stateful" (include "planner.fullname" .) -}}
{{- end }}
