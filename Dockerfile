FROM python:3.12-slim

WORKDIR /app

COPY local_server ./local_server

ENV PYTHONUNBUFFERED=1 \
    CHECKLIST_SERVER_HOST=0.0.0.0 \
    CHECKLIST_SERVER_PORT=8080 \
    CHECKLIST_SERVER_DATA_FILE=/data/checklists.json

VOLUME ["/data"]

EXPOSE 8080

CMD ["python", "local_server/server.py"]
