# JobMatcher

Prototype de plateforme de recrutement reliant candidats et offres d’emploi. Le projet explore la gestion de profils, l’extraction de compétences depuis un CV PDF et la recommandation d’offres.

> **Statut : prototype en cours de développement.** Ne pas utiliser en production ni avec des données personnelles réelles : l’authentification et la protection des données nécessitent un travail de sécurisation.

## Fonctionnalités présentes dans le code

- Profils candidats et recruteurs, offres et candidatures.
- Extraction de texte PDF avec Apache PDFBox et recherche de compétences par mots-clés.
- Classement des offres selon les compétences communes et la localisation.
- Interface web et assistant intégré ; persistance avec Neo4j.

## Technologies

Java 25 · Spring Boot · Neo4j · Apache PDFBox · HTML/CSS/JavaScript · Maven

## Structure

- [Code Java](JobMatcher/src/main/java) : application, contrôleurs et modèles.
- [Interface web](JobMatcher/src/main/resources/static).
- [Rapport du projet](JobMatcher/rapport).
- [Dépendances Maven](JobMatcher/pom.xml).

## Exécution locale

Le projet utilise Java 25 et une instance Neo4j locale. La configuration se trouve dans `JobMatcher/src/main/resources/application.properties`. Remplacez les identifiants existants par des identifiants locaux et ne les publiez pas. Depuis le dossier `JobMatcher`, la commande Maven prévue est :

```bash
mvn spring-boot:run
```

L’application est configurée sur le port 8080. Ces étapes n’ont pas été vérifiées dans un environnement neuf ; des dépendances ou configurations supplémentaires peuvent être nécessaires.

## Limites et prochaines étapes

La recommandation visible dans le contrôleur repose sur des correspondances de compétences et de ville ; elle ne constitue pas une évaluation validée des candidats. Avant tout usage réel : sécuriser l’authentification et l’accès aux données, retirer et renouveler les identifiants exposés, ajouter des tests et documenter la configuration complète.
