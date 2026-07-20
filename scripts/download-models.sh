#!/bin/bash
set -e

MODELS_DIR="${1:-${GAME_LIBRARY_CONFIG_DIR:-/gameLibrary/gameLibraryConfigs}/models}"
MODELS_DIR="$(realpath -m "$MODELS_DIR")"

echo "=== GameLibrary ONNX Models Downloader ==="
echo "Models will be saved to: $MODELS_DIR"
echo ""

check_python() {
    for ver in python3.12 python3.11 python3.10 python3; do
        if command -v "$ver" &>/dev/null; then
            local v=$("$ver" -c 'import sys; print(sys.version_info[:2])' 2>/dev/null)
            if echo "$v" | grep -qE '(3, 10)|(3, 11)|(3, 12)'; then
                PYTHON="$ver"
                echo "  Using $PYTHON ($( $PYTHON --version 2>&1 ))"
                return
            fi
        fi
    done
    echo "ERROR: Python 3.10-3.12 required but not found."
    echo "You have: $(python3 --version 2>/dev/null || echo 'no python3')"
    echo ""
    echo "Install Python 3.12:"
    echo "  sudo add-apt-repository -y ppa:deadsnakes/ppa"
    echo "  sudo apt install -y python3.12 python3.12-venv"
    exit 1
}

install_deps() {
    echo "Installing required Python packages..."
    $PYTHON -m pip install --quiet --only-binary :all: \
        "optimum[onnxruntime]>=1.25.0" \
        "transformers==4.46.3" \
        huggingface_hub
    if ! $PYTHON -m pip show optimum &>/dev/null; then
        echo "ERROR: Failed to install optimum. Try manually:"
        echo "  Install Python 3.12, then: $PYTHON -m pip install --only-binary :all: \"optimum[onnxruntime]>=1.25.0\" \"transformers==4.46.3\" huggingface_hub"
        exit 1
    fi
}

export_model() {
    local model_name="$1"
    local task="$2"
    local target_dir="$3"

    if [ -f "$target_dir/model.onnx" ] && [ -f "$target_dir/tokenizer.json" ]; then
        echo "  [SKIP] $model_name — already exists in $target_dir"
        return
    fi

    local task_label="${task:-auto}"
    echo "  Downloading & converting $model_name ($task_label) ..."
    mkdir -p "$target_dir"

    local task_args=()
    if [ -n "$task" ]; then
        task_args=(--task "$task")
    fi

    optimum-cli export onnx \
        --model "$model_name" \
        "${task_args[@]}" \
        --optimize O2 \
        "$target_dir"

    if [ -f "$target_dir/model.onnx" ]; then
        echo "  [OK] $model_name -> $target_dir"
    else
        echo "  [FAIL] $model_name — model.onnx not created in $target_dir"
        echo "  Check installed packages: $PYTHON -m pip show optimum onnxruntime huggingface_hub"
        exit 1
    fi
}

check_python
install_deps

echo ""
echo "Downloading models (total ~720 MB)..."
echo ""

export_model "intfloat/multilingual-e5-small" "sentence-similarity" "$MODELS_DIR/multilingual-e5-small"

export_model "Helsinki-NLP/opus-mt-ru-en" "" "$MODELS_DIR/opus-mt-ru-en"

export_model "Helsinki-NLP/opus-mt-en-ru" "" "$MODELS_DIR/opus-mt-en-ru"

echo ""
echo "=== Done! ==="
echo "Models installed to: $MODELS_DIR"
echo "Total size: $(du -sh "$MODELS_DIR" 2>/dev/null | cut -f1)"
echo ""
echo "Now restart the backend container:"
echo "  docker compose restart backend"
