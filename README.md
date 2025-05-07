### JDK Version: Recommend higher than or equal to JDK17

### How to install crawler JAR to local repo: (which is necessary for search-engine's execution
- Find crawler JAR under **crawler-1.1** and save to your local system
- Replace the path and run the following code at terminal:
- ```mvn install:install-file -Dfile=path/to/your-jar-file.jar -DgroupId=hk.ust.csit5930 -DartifactId=crawler -Dversion=1.1 -Dpackaging=jar```
- If installed successfully, you should find a **/Users/your-user-name/.m2/repository/hk/ust/csit5930/crawler/1.1** folder in Finder (for macOS users)

### How to run frontend
- First you need to have **npm** installed ([Tutorial with homebrew](https://phoenixnap.com/kb/install-npm-mac))
- Locate to where the **Wow! UI** sits, run the following commands in terminal: ```npm install```(you will only need to run this at the first time you start the project) and ```npm run dev```
- If you succeed to run the project, you shoule be able to see
- ```  VITE v6.3.3  ready in 204 ms
  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help```

### How to run backend
- It's straightforward. Simple click the run button in your IDE (if you've already installed the crawler JAR)
