---
name: project-documentation
description: "Documenta proyectos y servicios existentes a partir de evidencia del repositorio: resumen, descripción, stack tecnológico, flujos funcionales y técnicos en diagramas Mermaid, dependencias e integraciones. Úsala cuando se solicite documentar o entender un proyecto, servicio, arquitectura o sus flujos."
---

# Documentación de proyectos y servicios

## Propósito

Producir documentación técnica y funcional que ayude a comprender qué hace un proyecto o servicio, cómo se implementan sus recorridos principales y con qué componentes se integra. Mantener cada afirmación rastreable a evidencia o marcarla como pendiente de confirmar.

## Entradas

- Repositorio o ruta del proyecto.
- Servicio, capacidad o recorrido a documentar, si el repositorio contiene varios.
- Convenciones documentales vigentes y fuentes funcionales complementarias, si existen.

Si hay varios alcances posibles y no se puede determinar cuál solicitó el usuario, aclara el alcance antes de documentar.

## Salidas

Crea el archivo `ARCHITECTURE.md` en la raíz del proyecto, sin modificar el `README.md` ni otros archivos existentes. Si ya existe `ARCHITECTURE.md`, no lo sobrescribas: revisa su contenido y solicita confirmación antes de reemplazarlo o consolidar cambios.

Incluye estas secciones:

1. **Resumen:** propósito y capacidades principales.
2. **Descripción y alcance del servicio:** responsabilidades, actores o consumidores identificados y límites conocidos.
3. **Stack tecnológico:** tecnología, función, versión declarada y evidencia. Separa runtime, framework, almacenamiento, infraestructura y herramientas relevantes; identifica dependencias de desarrollo cuando aporten contexto.
4. **Flujos principales:** por cada recorrido importante:
   - **Flujo funcional:** actor o evento inicial, pasos y resultado.
   - **Flujo técnico:** componentes que procesan el recorrido y sus interacciones.
   - Diagramas Mermaid `flowchart` para visualizar el flujo y `sequenceDiagram` para mostrar la secuencia entre participantes.
5. **Dependencias e integraciones:** sistemas o servicios internos y externos, mecanismo de comunicación, dirección del intercambio y propósito, solo cuando haya evidencia.
6. **Evidencia y vacíos:** referencias a fuentes, supuestos que requieren confirmación y aspectos no identificados.

## Flujo

1. **Delimita el alcance.** Lee las instrucciones del repositorio y su documentación de entrada; identifica servicios, módulos y convenciones. Si el alcance sigue siendo ambiguo, pregunta antes de continuar.
2. **Construye un mapa de evidencia.** Inspecciona de forma selectiva los manifiestos de dependencias, configuración no sensible, contratos, rutas o puntos de entrada y el código que implementa los recorridos. Revisa también la documentación de despliegue si ayuda a confirmar runtime o integraciones. Omite artefactos generados y archivos que no aporten al alcance.
3. **Distingue certeza y fuente.** Para cada dato importante, anota la ruta relativa y, cuando sea posible, el símbolo, sección o propiedad que lo respalda. Clasifica lo encontrado como:
   - **Verificado:** confirmado en código, contrato o configuración vigente.
   - **Documentado, no verificado:** aparece en documentación, pero no se pudo contrastar con la implementación.
   - **Inferido:** conclusión razonable basada en evidencia indirecta; explica la base y solicita confirmación.
   - **No identificado:** no se encontró evidencia suficiente.
4. **Describe el servicio y su stack.** Resume responsabilidades y límites a partir de evidencia. Registra versiones solo si están declaradas; separa las tecnologías de ejecución de herramientas de desarrollo y no confundas una biblioteca con una integración entre sistemas.
5. **Traza los flujos principales.** Sigue cada recorrido desde el actor, petición o evento hasta su resultado, pasando por los límites entre capas y sistemas. Incluye decisiones o fallos solo si la implementación o los contratos muestran ese comportamiento. Prioriza recorridos representativos del propósito del servicio; no documentes cada función ni endpoint por defecto.
6. **Identifica dependencias e integraciones.** Separa dependencias de ejecución, herramientas de desarrollo e integraciones con sistemas. Para cada integración, registra nombre, tipo (interna/externa), propósito, dirección, mecanismo o protocolo y evidencia. Describe los datos intercambiados a nivel conceptual, sin copiar ejemplos sensibles. Si el mecanismo o la dirección no se puede confirmar, indícalo como no identificado.
7. **Dibuja los diagramas.** Usa `flowchart TD` para el recorrido funcional y `sequenceDiagram` para la interacción técnica correspondiente. Nombra actores y componentes de forma consistente con el código; usa fragmentos `alt` o `opt` para alternativas únicamente cuando estén implementadas. Marca límites de sistemas externos y no agregues pasos, mensajes ni secuencias especulativas.
8. **Genera el documento independiente.** Crea `ARCHITECTURE.md` en la raíz del proyecto con las secciones requeridas. No edites, reemplaces ni agregues enlaces al `README.md`; tampoco modifiques otros documentos existentes. Si hay información relacionada en esos archivos, úsala como fuente y referencia su ruta en `ARCHITECTURE.md`.
9. **Verifica antes de entregar.** Comprueba las referencias a archivos, la consistencia entre texto y diagramas y que cada flujo tenga su vista funcional y técnica. Revisa la sintaxis Mermaid con las herramientas ya disponibles en el repositorio; si no hay validador, haz una revisión estática y no agregues dependencias. Enumera los vacíos restantes.

## Constraints

- No inventar capacidades, actores, tecnologías, versiones, componentes, protocolos ni integraciones.
- No presentar una inferencia o una descripción documental como comportamiento verificado.
- No incluir secretos, tokens, credenciales, datos personales ni valores sensibles. Se pueden mencionar nombres de variables o propiedades de configuración, nunca sus valores secretos.
- No ejecutar el servicio, acceder a sistemas externos ni instalar herramientas para obtener evidencia.
- Limitar los cambios a documentación; no modificar código, dependencias, configuración funcional ni infraestructura.
- Si falta evidencia para un dato solicitado, escribir **“No identificado en el repositorio”** e indicar qué fuente permitiría confirmarlo; no completar con una suposición.

## Adaptación

- Escribe en el idioma del usuario. El archivo de salida es siempre `ARCHITECTURE.md` en la raíz del proyecto, independientemente de las convenciones de nombre documental existentes.
- Si el sistema es monolítico, documenta módulos y límites internos sin denominarlos servicios independientes. Si es distribuido, documenta por separado los servicios identificables y sus interacciones.
- Ajusta el detalle al alcance: céntrate en los flujos principales y dependencias directas relevantes; evita inventarios exhaustivos de dependencias transitivas.
- Si una integración solo aparece en configuración o documentación, registra esa evidencia y aclara que no se confirmó su uso efectivo en el flujo.
