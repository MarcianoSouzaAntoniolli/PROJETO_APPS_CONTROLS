"""
==========================================================
  ScanProduto - Servidor Mock da API REST
  Tecnologia: Python + Flask
  
  Como rodar:
    pip install flask
    python mock_server.py
  
  A API ficará disponível em: http://localhost:8080
  Configure o app com URL: http://<SEU_IP>:8080/
==========================================================
"""

from flask import Flask, jsonify, request, abort
import json

app = Flask(__name__)

# ============================================================
# Banco de dados em memória (mock)
# ============================================================
PRODUTOS_DB = {
    "7891234567890": {
        "ean": "7891234567890",
        "codigo": "001",
        "descricao": "Arroz Branco Tipo 1 - 5kg",
        "estoque": 50,
        "preco": 29.90
    },
    "7890000000001": {
        "ean": "7890000000001",
        "codigo": "002",
        "descricao": "Feijão Carioca - 1kg",
        "estoque": 8,   # Estoque baixo para testar alerta
        "preco": 8.50
    },
    "7891000100103": {
        "ean": "7891000100103",
        "codigo": "003",
        "descricao": "Óleo de Soja Refinado - 900ml",
        "estoque": 120,
        "preco": 7.99
    },
    "78912345": {
        "ean": "78912345",
        "codigo": "004",
        "descricao": "Chiclete Trident Menta (EAN-8)",
        "estoque": 200,
        "preco": 2.50
    },
    "7896001281069": {
        "ean": "7896001281069",
        "codigo": "005",
        "descricao": "Sabão em Pó OMO 1kg",
        "estoque": 0,   # Sem estoque para testar
        "preco": 19.90
    }
}


# ============================================================
# Endpoints
# ============================================================

@app.route("/produto/<string:ean>", methods=["GET"])
def get_produto_por_ean(ean):
    """
    Busca produto pelo código EAN.
    GET /produto/{ean}
    """
    produto = PRODUTOS_DB.get(ean)
    if not produto:
        return jsonify({"erro": f"Produto com EAN {ean} não encontrado"}), 404
    return jsonify(produto), 200


@app.route("/produto/codigo/<string:codigo>", methods=["GET"])
def get_produto_por_codigo(codigo):
    """
    Busca produto pelo código interno.
    GET /produto/codigo/{codigo}
    """
    produto = next(
        (p for p in PRODUTOS_DB.values() if p["codigo"] == codigo),
        None
    )
    if not produto:
        return jsonify({"erro": f"Produto com código {codigo} não encontrado"}), 404
    return jsonify(produto), 200


@app.route("/produtos", methods=["GET"])
def listar_produtos():
    """
    Lista todos os produtos (com paginação simples).
    GET /produtos?page=0&size=50
    """
    page = int(request.args.get("page", 0))
    size = int(request.args.get("size", 50))

    todos = list(PRODUTOS_DB.values())
    inicio = page * size
    fim = inicio + size
    pagina = todos[inicio:fim]

    return jsonify(pagina), 200


@app.route("/produto", methods=["POST"])
def salvar_produto():
    """
    Cria ou atualiza um produto.
    POST /produto
    Body: JSON do produto
    """
    dados = request.get_json()
    if not dados or "ean" not in dados:
        return jsonify({"erro": "Dados inválidos ou EAN ausente"}), 400

    ean = dados["ean"]
    PRODUTOS_DB[ean] = dados
    print(f"[POST] Produto salvo: {ean} - {dados.get('descricao')}")
    return jsonify(dados), 201


@app.route("/produto/busca", methods=["GET"])
def buscar_por_descricao():
    """
    Busca produtos por parte da descrição.
    GET /produto/busca?descricao=termo
    """
    termo = request.args.get("descricao", "").lower().strip()
    if not termo:
        return jsonify([]), 200
    resultados = [p for p in PRODUTOS_DB.values() if termo in p["descricao"].lower()]
    return jsonify(resultados), 200


@app.route("/produto/<string:ean>", methods=["PUT"])
def atualizar_produto(ean):
    """
    Atualiza um produto existente.
    PUT /produto/{ean}
    """
    if ean not in PRODUTOS_DB:
        return jsonify({"erro": f"Produto {ean} não encontrado"}), 404

    dados = request.get_json()
    PRODUTOS_DB[ean].update(dados)
    print(f"[PUT] Produto atualizado: {ean}")
    return jsonify(PRODUTOS_DB[ean]), 200


@app.route("/produtos/batch", methods=["POST"])
def salvar_batch():
    """
    Cadastra múltiplos produtos de uma vez.
    POST /produtos/batch
    Body: lista JSON de produtos
    """
    lista = request.get_json()
    if not isinstance(lista, list):
        return jsonify({"erro": "Esperado array de produtos"}), 400

    salvos = 0
    for produto in lista:
        if "ean" in produto:
            PRODUTOS_DB[produto["ean"]] = produto
            salvos += 1

    print(f"[BATCH] {salvos} produtos salvos")
    return jsonify({"total": salvos, "mensagem": f"{salvos} produtos processados"}), 201


# ============================================================
# Inicialização
# ============================================================

if __name__ == "__main__":
    print("=" * 50)
    print("  ScanProduto - Mock API Server")
    print("  Rodando em: http://0.0.0.0:8080")
    print("  Produtos disponíveis:")
    for ean, p in PRODUTOS_DB.items():
        print(f"    {ean} -> {p['descricao']}")
    print("=" * 50)

    app.run(host="0.0.0.0", port=8080, debug=True)
