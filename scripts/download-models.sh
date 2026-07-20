#!/bin/bash
set -e

MODELS_DIR="${1:-${GAME_LIBRARY_CONFIG_DIR:-/gameLibrary/gameLibraryConfigs}/models}"

echo "=== GameLibrary ONNX Models Downloader ==="
echo "Models will be saved to: $MODELS_DIR"
echo ""

check_python() {
    if command -v python3 &>/dev/null; then
        PYTHON=python3
    elif command -v python &>/dev/null; then
        PYTHON=python
    else
        echo "ERROR: Python 3 not found. Install it first:"
        echo "  Ubuntu/Debian: sudo apt install python3 python3-pip"
        echo "  CentOS/RHEL:   sudo yum install python3 python3-pip"
        echo "  TOS/TerraMaster: install Python from App Center"
        exit 1
    fi
}

install_deps() {
    echo "Installing required Python packages..."
    $PYTHON -m pip install --quiet optimum-cli[onnxruntime] huggingface_hub 2>&1 | tail -1
}

export_model() {
    local model_name="$1"
    local task="$2"
    local target_dir="$3"

    if [ -f "$target_dir/model.onnx" ] && [ -f "$target_dir/tokenizer.json" ]; then
        echo "  [SKIP] $model_name — already exists in $target_dir"
        return
    fi

    echo "  Downloading & converting $model_name ..."
    mkdir -p "$target_dir"
    $PYTHON -m optimum_cli export onnx \
        --model "$model_name" \
        --task "$task" \
        --optimize O2 \
        "$target_dir" 2>&1 | tail -3
    echo "  [OK] $model_name -> $target_dir"
}

check_python
install_deps

echo ""
echo "Downloading models (total ~720 MB)..."
echo ""

export_model "intfloat/multilingual-e5-small" "sentence-similarity" "$MODELS_DIR/multilingual-e5-small"

export_model "Helsinki-NLP/opus-mt-ru-en" "translation" "$MODELS_DIR/opus-mt-ru-en"

export_model "Helsinki-NLP/opus-mt-en-ru" "translation" "$MODELS_DIR/opus-mt-en-ru"

echo ""
echo "=== Done! ==="
echo "Models installed to: $MODELS_DIR"
echo "Total size: $(du -sh "$MODELS_DIR" 2>/dev/null | cut -f1)"
echo ""
echo "Now restart the backend container:"
echo "  docker compose restart backend"
