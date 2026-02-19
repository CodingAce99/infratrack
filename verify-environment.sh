#!/bin/bash

# ═══════════════════════════════════════════════════════════════════
# INFRATRACK - ENVIRONMENT VERIFICATION SCRIPT
# ═══════════════════════════════════════════════════════════════════
#
# Este script verifica que tienes todo lo necesario para desarrollar.
#
# USO:
#   chmod +x verify-environment.sh
#   ./verify-environment.sh
#
# ═══════════════════════════════════════════════════════════════════

set -e

echo "═══════════════════════════════════════════════════════════════════"
echo "🔍 VERIFICANDO ENTORNO DE DESARROLLO - INFRATRACK"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SUCCESS=0

# ───────────────────────────────────────────────────────────────────
# 1. Java 21
# ───────────────────────────────────────────────────────────────────
echo "1️⃣  Verificando Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -eq 21 ]; then
        echo -e "${GREEN}✅ Java 21 encontrado${NC}"
        java -version 2>&1 | head -n 1
    else
        echo -e "${RED}❌ Java $JAVA_VERSION encontrado, pero necesitas Java 21${NC}"
        echo "   Descarga: https://adoptium.net/"
        SUCCESS=1
    fi
else
    echo -e "${RED}❌ Java no encontrado${NC}"
    echo "   Instala Java 21 desde: https://adoptium.net/"
    SUCCESS=1
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 2. Maven
# ───────────────────────────────────────────────────────────────────
echo "2️⃣  Verificando Maven..."
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}✅ Maven encontrado${NC}"
    mvn -version | head -n 1
else
    echo -e "${YELLOW}⚠️  Maven no encontrado (no es crítico, puedes usar ./mvnw)${NC}"
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 3. Docker
# ───────────────────────────────────────────────────────────────────
echo "3️⃣  Verificando Docker..."
if command -v docker &> /dev/null; then
    if docker ps &> /dev/null; then
        echo -e "${GREEN}✅ Docker funcionando${NC}"
        docker --version
    else
        echo -e "${RED}❌ Docker instalado pero no está corriendo${NC}"
        echo "   Inicia Docker Desktop o: sudo systemctl start docker"
        SUCCESS=1
    fi
else
    echo -e "${RED}❌ Docker no encontrado${NC}"
    echo "   Instala desde: https://www.docker.com/get-started"
    SUCCESS=1
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 4. Docker Compose
# ───────────────────────────────────────────────────────────────────
echo "4️⃣  Verificando Docker Compose..."
if command -v docker-compose &> /dev/null || docker compose version &> /dev/null; then
    echo -e "${GREEN}✅ Docker Compose encontrado${NC}"
    if command -v docker-compose &> /dev/null; then
        docker-compose --version
    else
        docker compose version
    fi
else
    echo -e "${RED}❌ Docker Compose no encontrado${NC}"
    echo "   Viene incluido con Docker Desktop"
    SUCCESS=1
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 5. Git
# ───────────────────────────────────────────────────────────────────
echo "5️⃣  Verificando Git..."
if command -v git &> /dev/null; then
    echo -e "${GREEN}✅ Git encontrado${NC}"
    git --version
    
    # Verificar configuración de Git
    if git config user.name &> /dev/null && git config user.email &> /dev/null; then
        echo -e "${GREEN}   ✅ Git configurado con usuario: $(git config user.name)${NC}"
    else
        echo -e "${YELLOW}   ⚠️  Git no configurado completamente${NC}"
        echo "   Ejecuta:"
        echo "     git config --global user.name \"Tu Nombre\""
        echo "     git config --global user.email \"tu@email.com\""
    fi
else
    echo -e "${RED}❌ Git no encontrado${NC}"
    echo "   Instala desde: https://git-scm.com/"
    SUCCESS=1
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 6. IntelliJ (opcional pero recomendado)
# ───────────────────────────────────────────────────────────────────
echo "6️⃣  Verificando IntelliJ IDEA..."
if [ -d "/Applications/IntelliJ IDEA CE.app" ] || [ -d "/Applications/IntelliJ IDEA.app" ] || command -v idea &> /dev/null; then
    echo -e "${GREEN}✅ IntelliJ IDEA encontrado${NC}"
else
    echo -e "${YELLOW}⚠️  IntelliJ no detectado (puedes usar otro IDE)${NC}"
    echo "   Recomendado: https://www.jetbrains.com/idea/download/"
fi
echo ""

# ───────────────────────────────────────────────────────────────────
# 7. Puertos disponibles
# ───────────────────────────────────────────────────────────────────
echo "7️⃣  Verificando puertos disponibles..."

check_port() {
    PORT=$1
    NAME=$2
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an 2>/dev/null | grep LISTEN | grep -q ":$PORT "; then
        echo -e "${RED}   ❌ Puerto $PORT ($NAME) ya está en uso${NC}"
        return 1
    else
        echo -e "${GREEN}   ✅ Puerto $PORT ($NAME) disponible${NC}"
        return 0
    fi
}

check_port 8080 "Spring Boot" || SUCCESS=1
check_port 5432 "PostgreSQL" || SUCCESS=1
echo ""

# ───────────────────────────────────────────────────────────────────
# RESULTADO FINAL
# ───────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════════"
if [ $SUCCESS -eq 0 ]; then
    echo -e "${GREEN}✅ ENTORNO LISTO PARA DESARROLLO${NC}"
    echo ""
    echo "Siguiente paso:"
    echo "  1. mvn clean compile"
    echo "  2. docker-compose up -d postgres"
    echo "  3. mvn spring-boot:run -Dspring-boot.run.profiles=dev"
else
    echo -e "${RED}❌ HAY PROBLEMAS QUE RESOLVER${NC}"
    echo ""
    echo "Revisa los errores arriba y corrígelos antes de continuar."
fi
echo "═══════════════════════════════════════════════════════════════════"

exit $SUCCESS
