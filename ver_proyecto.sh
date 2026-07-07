#!/bin/bash
echo "========================================" > ~/mclauncher/resumen_proyecto.txt
echo "  LUCYMC - RESUMEN COMPLETO DEL PROYECTO" >> ~/mclauncher/resumen_proyecto.txt
echo "========================================" >> ~/mclauncher/resumen_proyecto.txt
echo "" >> ~/mclauncher/resumen_proyecto.txt

# Buscar archivos importantes
archivos=$(find ~/mclauncher/app/src -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.gradle" -o -name "*.json" -o -name "*.properties" \) 2>/dev/null)

for archivo in $archivos; do
    echo "" >> ~/mclauncher/resumen_proyecto.txt
    echo "========================================" >> ~/mclauncher/resumen_proyecto.txt
    echo "ARCHIVO: $archivo" >> ~/mclauncher/resumen_proyecto.txt
    echo "========================================" >> ~/mclauncher/resumen_proyecto.txt
    echo "" >> ~/mclauncher/resumen_proyecto.txt
    cat "$archivo" >> ~/mclauncher/resumen_proyecto.txt
    echo "" >> ~/mclauncher/resumen_proyecto.txt
done

echo "" >> ~/mclauncher/resumen_proyecto.txt
echo "========================================" >> ~/mclauncher/resumen_proyecto.txt
echo "RESUMEN GENERADO. FIN." >> ~/mclauncher/resumen_proyecto.txt
echo "========================================" >> ~/mclauncher/resumen_proyecto.txt

echo "✅ Resumen creado en: ~/mclauncher/resumen_proyecto.txt"
echo "📄 Tamaño: $(wc -l < ~/mclauncher/resumen_proyecto.txt) líneas"
echo ""
echo "Para verlo:"
echo "  cat ~/mclauncher/resumen_proyecto.txt"
echo ""
echo "Para copiarlo a Downloads (puedes abrirlo con Acode):"
echo "  cp ~/mclauncher/resumen_proyecto.txt /storage/emulated/0/Download/"
