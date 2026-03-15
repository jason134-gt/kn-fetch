#!/usr/bin/env python3
import uvicorn
from src.web.app import app

if __name__ == "__main__":
    print("Server starting at http://127.0.0.1:8000")
    uvicorn.run(app, host="127.0.0.1", port=8000)
