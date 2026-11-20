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

Para garantizar la estabilidad y fiabilidad en cada despliegue, el ciclo de vida del microservicio esta automatizado mediante GitHub Actions (`ci.yml`). El pipeline consta de las siguientes etapas:
  **Construccion, Pruebas Automatizadas y Reporte HTML:** Se compila el codigo utilizando Maven y JDK 21, ejecutando automaticamente las pruebas para asegurar que los nuevos cambios no rompan la logica de negocio y haciendo que el job falle de inmediato si alguna prueba falla. Para que los tests corran en la nube sin depender de una base de datos MySQL local, ocupe `@WebMvcTest` con `@MockitoBean` en `PeliculaControllerTest` para aislar el controlador[cite: 1], e inyecte directamente las propiedades de una base de datos en memoria **H2** en `PeliculasApplicationTests`[cite: 1]. Ademas, configure el plugin de Surefire para generar el reporte de pruebas en formato HTML y subirlo automaticamente como un artefacto descargable (`upload-artifact`) desde GitHub Actions[cite: 2].

  **Analisis de Seguridad Estatico (SAST) y Escaneo de Dependencias (SCA):** Se integra **SonarCloud** para escanear el codigo fuente en busqueda de vulnerabilidades, *security hotspots* y *code smells* pasando por el Quality Gate[cite: 5]. Ademas, integre **Snyk** dentro del pipeline para analizar el archivo `pom.xml`[cite: 1] y detectar vulnerabilidades de seguridad en las librerias del proyecto. Adicionalmente, **Dependabot** alerta sobre dependencias y acciones desactualizadas[cite: 1, 6].

  **Contenerizacion:** Se utiliza un `Dockerfile` multi-etapa (multi-stage build) para empaquetar el microservicio de manera reproducible y generar una imagen optimizada.
  **Despliegue y Registro:** Tras pasar las pruebas y los analisis de seguridad, la imagen Docker se construye y sube automaticamente al GitHub Container Registry (GHCR).

***Orquestacion***
La orquestacion local se maneja mediante un archivo `docker-compose.yml`[cite: 1], el cual levanta el microservicio y el contenedor de la base de datos MySQL funcionando en conjunto. Para dejarlo bien configurado, saque las credenciales a un archivo `.env` por seguridad[cite: 1], agregue volumenes (`volumes`) para que los datos de MySQL persistan si se apaga el contenedor, e implemente un `healthcheck` combinado con `depends_on` para asegurar que el microservicio espere a que la base de datos este cien por ciento lista antes de arrancar.

---
*** Conclusiones y Reflexiones Personales ***


**Reflexion **
En lo personal, lo que mas me hizo pensar en esta evaluacion fue configurar bien GitHub Actions y lograr que las pruebas pasaran en la nube. Al principio el pipeline no me arrancaba cuando subia cambios a la rama main, asi que tuve que meterme a editar el archivo `ci.yml` a mano para arreglar los triggers. Despues, cuando habilite las pruebas, el pipeline se caia porque seguia buscando el dialecto de MySQL, asi que aprender a inyectar H2 directo en la clase de prueba y rescatar la carpeta `target` para descargar el reporte HTML me ayudo mucho a entender como funcionan los entornos aislados. Tambien aprendi harto sobre seguridad: en vez de dejar credenciales tiradas en el codigo, entendi como usar el archivo `.env` en local[cite: 1] y los Secrets de GitHub para guardar los tokens de SonarCloud y Snyk de forma segura. Al final, me di cuenta de que manejar Git directamente desde la interfaz de Visual Studio Code es mucho mas comodo y te salva de los tipicos errores por teclear mal en la terminal[cite: 1].

** Conclusion Final **
En resumen, sacar adelante este proyecto me sirvio harto para entender como se trabaja realmente en el mundo DevOps. Al principio costo un poco armar todo el puzzle entre GitHub, las pruebas con H2, SonarCloud, Snyk y Docker Compose[cite: 1, 5], pero una vez que el pipeline quedo en verde, me di cuenta del tremendo alivio que es tener todo automatizado. Ya no hay que estar probando a mano ni adivinando si el codigo va a fallar al subirlo. Aplicar GitFlow junto con estas herramientas de integracion continua, pruebas y seguridad me deja una base super solida para no romper la produccion y trabajar de forma mucho mas profesional de aqui en adelante[cite: 3].