/**
 * Motor ambiental suave para Campusland Ops
 * Nodos espaciales sutiles sin inclinaciones bruscas en las tarjetas.
 */

export function inicializarEfectos3D() {
    iniciarCanvasEspacialSuave();
}

function iniciarCanvasEspacialSuave() {
    const canvas = document.getElementById('bgCanvas3D');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    let width = (canvas.width = window.innerWidth);
    let height = (canvas.height = window.innerHeight);

    window.addEventListener('resize', () => {
        width = canvas.width = window.innerWidth;
        height = canvas.height = window.innerHeight;
    });

    const NUM_NODES = 38;
    const nodes = Array.from({ length: NUM_NODES }, () => ({
        x: Math.random() * width,
        y: Math.random() * height,
        vx: (Math.random() - 0.5) * 0.25,
        vy: (Math.random() - 0.5) * 0.25,
        radius: Math.random() * 1.6 + 1
    }));

    function esModoClaro() {
        const meta = document.querySelector('meta[name="color-scheme"]');
        return meta && meta.content === 'light';
    }

    function render() {
        ctx.clearRect(0, 0, width, height);
        const light = esModoClaro();

        for (let i = 0; i < nodes.length; i++) {
            const n = nodes[i];
            n.x += n.vx;
            n.y += n.vy;

            if (n.x < 0 || n.x > width) n.vx *= -1;
            if (n.y < 0 || n.y > height) n.vy *= -1;
        }

        for (let i = 0; i < nodes.length; i++) {
            for (let j = i + 1; j < nodes.length; j++) {
                const a = nodes[i];
                const b = nodes[j];
                const dist = Math.hypot(a.x - b.x, a.y - b.y);

                if (dist < 150) {
                    const alpha = (1 - dist / 150) * (light ? 0.12 : 0.18);
                    ctx.strokeStyle = light
                        ? `rgba(37, 99, 235, ${alpha})`
                        : `rgba(56, 189, 248, ${alpha})`;
                    ctx.lineWidth = 1;
                    ctx.beginPath();
                    ctx.moveTo(a.x, a.y);
                    ctx.lineTo(b.x, b.y);
                    ctx.stroke();
                }
            }
        }

        for (let i = 0; i < nodes.length; i++) {
            const n = nodes[i];
            ctx.beginPath();
            ctx.arc(n.x, n.y, n.radius, 0, Math.PI * 2);
            ctx.fillStyle = light
                ? 'rgba(37, 99, 235, 0.28)'
                : 'rgba(125, 211, 252, 0.38)';
            ctx.fill();
        }

        requestAnimationFrame(render);
    }

    requestAnimationFrame(render);
}
