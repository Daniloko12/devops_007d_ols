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