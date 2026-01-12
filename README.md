# Sistem Monitorizare Senzori cu Agenți JADE & AI

Acest proiect implementează un sistem distribuit multi-agent (MAS) folosind platforma **JADE** (Java Agent Development Framework). Sistemul simulează monitorizarea unor parametri de mediu, analizează datele critic folosind inteligență artificială (**Ollama / Llama 3.2**) și arhivează istoricul.

##  Echipa și Responsabilități

Proiectul este împărțit în două module majore:

1.  **Modul Intrare & Ieșire (Student 1 - Costeniuc Daniel):**
    * **`MultiSenzorAgent`**: Simulator GUI pentru senzorii de Temperatură, Umiditate și Presiune. Trimite date periodic.
    * **`LoggerAgent`**: Agent specializat pe scrierea datelor pe disc (`istoric_senzori.txt`) și gestionarea logurilor.

2.  **Modul Procesare & AI (Student 2 - Ciobanu Gabriel):**
    * **`ControllerAgent`**: "Creierul" sistemului. Primește datele, decide dacă e pericol și trimite alerte vizuale.
    * **`OllamaService`**: Conexiune HTTP către LLM-ul local pentru a genera mesaje de alertă contextuale ("Panică").

---

##  Arhitectura Sistemului

Fluxul de date este următorul:

1.  **Senzori** -> citesc valorile din Slidere (GUI) la fiecare 8 secunde.
2.  **Senzori** -> trimit JSON către **Controller**.
3.  **Controller** -> verifică pragurile (ex: Temp > 50°C).
    * *Dacă e pericol:* Cere AI-ului (Ollama) un text scurt de alertă.
4.  **Controller** -> trimite datele procesate către **Logger**.
5.  **Logger** -> scrie în fișierul `istoric_senzori.txt`.

---

##  Instrucțiuni de Instalare și Rulare

Urmați acești pași exacți pentru a configura mediul și a porni agenții.

### Pasul 1: Pregătire AI (Ollama)
1.  Descarcă și instalează [Ollama](https://ollama.com/).
2.  Deschide un terminal (CMD sau PowerShell).
3.  Descarcă modelul necesar rulând comanda:
    ```cmd
    ollama pull llama3.2
    ```
4.  (Opțional) Asigură-te că serviciul este pornit scriind `ollama run llama3.2` (apoi poți ieși cu `/bye`, serverul rămâne activ în fundal).

### Pasul 2: Configurare Proiect în IntelliJ
1.  Deschide IntelliJ IDEA.
2.  Mergi la `File` -> `Project Structure` -> `Libraries`.
3.  Adaugă fișierul `jade.jar` (se găsește în folderul `/lib` al acestui proiect).
4.  Dă **Apply** și **OK**.

### Pasul 3: Creare Configurație de Rulare
1.  În dreapta sus, apasă pe meniul de rulare și selectează **Edit Configurations...**.
2.  Apasă **+** și alege **Application**.
3.  Completează câmpurile astfel:

    * **Main Class:**
        ```text
        jade.Boot
        ```
    * **Program Arguments:**
        *(Copiază exact linia de mai jos)*
        ```text
        -gui -agents "Controller:ControllerAgent;Logger:LoggerAgent;Senzori:MultiSenzorAgent"
        ```

### Pasul 4: Start
1.  Dă **Apply**, apoi **OK**.
2.  Apasă butonul **Run** (Play).
3.  Se vor deschide interfața JADE și ferestrele agenților (Senzori și Controller).

