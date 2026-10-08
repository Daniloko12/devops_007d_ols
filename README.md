*** Documentacion del Microservicio - Proyecto DevOps EP1 ***

*** Descripcion del Proyecto ***
Aqui dejo el microservicio base que ocupe para la Evaluacion Parcial 1. Esta hecho en Java 21, utilizando el patron Controller-Service-Repository. Ocupe esta estructura porque me facilita harto separar las responsabilidades del codigo y mantener todo higienico y ordenado.

*** Estrategia de Ramas: Por que ocupe GitFlow?  *** 
Elegi GitFlow principalmente porque me permite trabajar de forma super ordenada y segura. Me facilito mucho la vida poder separar lo que esta listo para produccion (en la rama `main`) del codigo en el que estoy trabajando dia a dia (en la rama `develop`).
 Para cosas nuevas: Cuando tuve que agregar herramientas o configuraciones, simplemente saque ramas `feature/` desde `develop`. Esto me permitio avanzar sin miedo a romper el codigo principal.
 
 Para emergencias: Si llega a fallar algo critico en produccion, esta estrategia me permite sacar una rama `hotfix/` directamente desde `main`, arreglarlo rapido y subirlo sin enredarme con el desarrollo que esta a medias.

*** Trazabilidad y Flujo Colaborativo ***
Para mantener el repositorio limpio y sin basura, segui la convencion de mensajes de commit (usando etiquetas como feat:, fix:, docs:, chore:, ci:). Esto me permitio tener un historial clarito donde se entiende perfecto que hice en cada paso. Ademas, simule el trabajo colaborativo integrando los cambios siempre mediante Pull Requests hacia `develop`, asegurando que el codigo pase por una revision antes del merge.

*** Automatizacion Continua (CI/CD) ***
Configure GitHub Actions creando un archivo `ci.yml`. Ocupe esta herramienta porque me automatiza la verificacion del codigo. Cada vez que tiro un push a `develop` o armo un Pull Request a `main`, el pipeline de GitHub compila el microservicio en la nube por mi cuenta. Me permite estar cien por ciento seguro de que no estoy subiendo codigo roto a produccion.

*** Declaracion de uso de Inteligencia Artificial (IA) ***
Para el desarrollo de este encargo, utilice Inteligencia Artificial (Gemini) como apoyo. Especificamente, ocupe la IA para que me guiara con descripciones y el paso a paso de los comandos de Git en la terminal, ya que acordarme de la sintaxis exacta de los comandos y de como conectar el repositorio fue lo que me costo un poco mas de lo normal. Todo el analisis de la arquitectura, revision del codigo y ejecucion fue validado por mi.


*** Trazabilidad y Calidad del Pipeline ***

Para garantizar la estabilidad y fiabilidad en cada despliegue, el ciclo de vida del microservicio está automatizado mediante GitHub Actions (`ci.yml`). El pipeline consta de las siguientes etapas:
  **Construcción y Pruebas Unitarias:** Se compila el código utilizando Maven y JDK 21, ejecutando automáticamente las pruebas unitarias para asegurar que los nuevos cambios no rompan la lógica de negocio.

  **Análisis de Seguridad Estático (SAST):** Se integra **SonarCloud** para escanear el código en búsqueda de vulnerabilidades y *code smells*. Adicionalmente, **Dependabot** alerta sobre dependencias desactualizadas.

  **Contenerización:** Se utiliza un `Dockerfile` multi-etapa (multi-stage build) para generar una imagen optimizada.
  **Despliegue y Registro:** Tras el análisis exitoso, la imagen Docker se construye y sube automáticamente al GitHub Container Registry (GHCR).

***Orquestación***
La orquestación local se maneja mediante un archivo `docker-compose.yml`, el cual define la configuración, puertos expuestos y variables de entorno necesarias para levantar el microservicio de manera aislada.

---
*** Conclusiones y Reflexiones Personales ***


**Reflexión **
En lo personal, lo que más me hizo pensar en esta evaluación fue configurar bien GitHub Actions. Al principio el pipeline no me arrancaba cuando subía cambios a la rama main, así que tuve que meterme a editar el archivo `ci.yml` a mano para arreglar los triggers. También aprendí harto sobre seguridad: en vez de dejar credenciales tiradas en el código, entendi cómo usar los Secrets de GitHub para guardar el token de SonarCloud de forma segura. Al final, me di cuenta de que manejar Git directamente desde la interfaz de Visual Studio Code es mucho más cómodo y te salva de los típicos errores por teclear mal en la terminal.

** Conclusión Final **
En resumen, sacar adelante este proyecto nos sirvió harto para entender cómo se trabaja realmente en el mundo DevOps. Al principio costó un poco armar todo el puzzle entre GitHub, SonarCloud y Docker, pero una vez que el pipeline quedó en verde, nos dimos cuenta del tremendo alivio que es tener todo automatizado. Ya no hay que estar probando a mano ni adivinando si el código va a fallar al subirlo. Aplicar GitFlow junto con estas herramientas de integración continua nos deja una base súper sólida para no romper la producción y trabajar de forma mucho más profesional de aquí en adelante.