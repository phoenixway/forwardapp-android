import os
import threading
import time
from pathlib import Path
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel

app = FastAPI(title="Scoped Storage File API")

# ==============================
# Конфіг
# ==============================
ROOT_DIRECTORY = Path("/home/romankozak/Documents/shared-from-fam")
ROOT_DIRECTORY.mkdir(parents=True, exist_ok=True)

# ==============================
# Моделі
# ==============================
class FileData(BaseModel):
    filename: str
    content: str

# ==============================
# Допоміжні функції
# ==============================
def get_subdir(subdir: str | None) -> Path:
    """Гарантує що робота тільки в межах ROOT_DIRECTORY"""
    if subdir:
        safe_subdir = Path(subdir).name  # не даємо "../../"
        target = ROOT_DIRECTORY / safe_subdir
    else:
        target = ROOT_DIRECTORY
    target.mkdir(parents=True, exist_ok=True)
    return target

# ==============================
# API
# ==============================
@app.post("/api/v1/files")
def create_or_update_file(
    file_data: FileData,
    subdir: str | None = Query(default=None, description="Підпапка у storage")
):
    """Записати файл у підпапку"""
    folder = get_subdir(subdir)
    file_path = folder / f"{file_data.filename}.md"
    existed = file_path.exists()
    file_path.write_text(file_data.content, encoding="utf-8")
    action = "оновлено" if existed else "створено"
    return {"status": "success", "message": f"Файл '{file_path}' {action}."}


@app.get("/api/v1/files")
def list_files(
    subdir: str | None = Query(default=None, description="Підпапка у storage")
):
    """Список файлів у підпапці"""
    folder = get_subdir(subdir)
    files = [f.name for f in folder.glob("*.md")]
    return {"directory": str(folder), "files": files}


@app.get("/api/v1/files/{filename}")
def read_file(
    filename: str,
    subdir: str | None = Query(default=None, description="Підпапка у storage")
):
    """Прочитати файл із підпапки"""
    folder = get_subdir(subdir)
    file_path = folder / f"{filename}.md"
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="Файл не знайдено")
    content = file_path.read_text(encoding="utf-8")
    return {"filename": file_path.name, "content": content}


@app.post("/api/v1/backlog")
def upload_backlog(
    subdir: str | None = Query(default=None, description="Підпапка у storage")
):
    folder = get_subdir(subdir)
    return {"status": "success", "message": f"Backlog uploaded to '{folder}'"}


@app.post("/api/v1/stop")
def stop_server():
    """Зупинити сервер"""
    def shutdown():
        time.sleep(0.5)
        os._exit(0)
    threading.Thread(target=shutdown).start()
    return {"status": "success", "message": "Сервер зупиняється..."}


import httpx

@app.get("/api/tags")
def get_tags():
    """Fetch available models from Ollama server."""
    try:
        response = httpx.get("http://localhost:11434/api/tags")
        response.raise_for_status()
        return response.json()
    except httpx.RequestError as e:
        raise HTTPException(status_code=500, detail=f"Error connecting to Ollama: {e}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {e}")


if __name__ == "__main__":
    import uvicorn
    import socket
    from zeroconf import ServiceInfo, Zeroconf

    def run_server():
        uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)

    # Створюємо та запускаємо сервер в окремому потоці
    server_thread = threading.Thread(target=run_server)
    server_thread.daemon = True
    server_thread.start()

    # Реєструємо сервіс mDNS
    zeroconf = Zeroconf()
    service_name = "ForwardApp FAM"
    service_type = "_http._tcp.local."
    
    # Визначаємо IP адресу
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('10.255.255.255', 1))
        ip_address = s.getsockname()[0]
    except Exception:
        ip_address = '127.0.0.1'
    finally:
        s.close()

    print(f"Starting mDNS service '{service_name}' at {ip_address}:8000")

    service_info = ServiceInfo(
        service_type,
        f"{service_name}.{service_type}",
        addresses=[socket.inet_aton(ip_address)],
        port=8000,
        properties={'path': '/api/v1'},
        server="fam-server.local.",
    )

    zeroconf.register_service(service_info)

    try:
        # Чекаємо на завершення роботи сервера (наприклад, через /api/v1/stop)
        while server_thread.is_alive():
            time.sleep(1)
    finally:
        print("Unregistering mDNS service...")
        zeroconf.unregister_service(service_info)
        zeroconf.close()
        print("Server stopped.")
